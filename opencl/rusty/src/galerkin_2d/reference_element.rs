extern crate rulinalg;

use self::rulinalg::matrix::{BaseMatrix, Matrix};
use self::rulinalg::vector::Vector;
use functions::jacobi_polynomials;
use functions::vandermonde;
use std::f64::consts::PI;
use std::iter::FromIterator;
use galerkin_2d::grid::FaceNumber;

const ALPHAS: [f64; 15] = [
    0.0000, 0.0000, 1.4152, 0.1001, 0.2751, 0.9800, 1.0999, 1.2832, 1.3648, 1.4773, 1.4959, 1.5743,
    1.5770, 1.6223, 1.6258,
];
const EPSILON: f64 = 1.0e-12;

#[derive(Debug, PartialEq)]
pub struct ReferencePoint {
    r: f64,
    s: f64,
}

#[derive(Debug)]
pub struct ReferenceElement {
    pub n: i32,
    pub n_p: usize,

    pub points: Vec<ReferencePoint>,
    pub rs: Vector<f64>,
    pub ss: Vector<f64>,

    pub face1: Vec<usize>,
    pub face2: Vec<usize>,
    pub face3: Vec<usize>,
}

impl ReferenceElement {
    pub fn face(&self, face_number: FaceNumber) -> &Vec<usize> {
        match face_number {
            FaceNumber::One => &self.face1,
            FaceNumber::Two => &self.face2,
            FaceNumber::Three => &self.face3,
        }
    }

    pub fn legendre(n: i32) -> ReferenceElement {
        let n_p = (n + 1) * (n + 2) / 2;
        let (x, y) = ReferenceElement::equilateral_nodes(n);
        let root_3 = 3.0_f64.sqrt();
        let L1 = &(&y * root_3 + 1.) / 3.;
        let L2 = &(&(&x * -3.) - &(&y * root_3) + 2.0) / 6.;
        let L3 = &(&(&x * 3.) - &(&y * root_3) + 2.0) / 6.;

        let rs: Vector<f64> = -&L2 + &L3 - &L1;
        let ss: Vector<f64> = -&L2 - &L3 + &L1;
        let face1: Vec<usize> = (0..rs.size())
            .into_iter()
            .filter(|i| (ss[*i] + 1.).abs() < EPSILON)
            .collect();
        let face2: Vec<usize> = (0..rs.size())
            .into_iter()
            .filter(|i| (rs[*i] + ss[*i]).abs() < EPSILON)
            .collect();
        let face3: Vec<usize> = (0..rs.size())
            .into_iter()
            .filter(|i| (rs[*i] + 1.).abs() < EPSILON)
            .collect();

        let points: Vec<ReferencePoint> = rs
            .iter()
            .zip(ss.iter())
            .map(|(&r, &s)| ReferencePoint { r, s })
            .collect();
        ReferenceElement {
            n,
            n_p: n_p as usize,
            points,
            rs,
            ss,
            face1,
            face2,
            face3,
        }
    }

    fn equilateral_nodes(n: i32) -> (Vector<f64>, Vector<f64>) {
        let nf = n as f64;
        let alpha = if n < 16 { ALPHAS[n as usize - 1] } else { 1.6 };

        // total number of nodes
        let n_p = (n + 1) * (n + 2) / 2;
        println!("np: {}", n_p);

        let mut L1 = Vector::zeros(n_p as usize);
        let mut L3 = Vector::zeros(n_p as usize);
        let mut sk = 0;
        (0..n + 1).into_iter().for_each(|i| {
            (0..n + 1 - i).into_iter().for_each(|j| {
                L1[sk] = (i as f64) / nf;
                L3[sk] = (j as f64) / nf;
                sk = sk + 1;
            })
        });
        let L2 = &L1 * -1. - &L3 + 1.;
        let x = &L3 - &L2;
        let y = (&L1 * 2. - &L2 - &L3) / 3.0_f64.sqrt();

        // Blending functions
        let blend_1 = &(L2.elemul(&L3)) * 4.;
        let blend_2 = &(L1.elemul(&L3)) * 4.;
        let blend_3 = &(L1.elemul(&L2)) * 4.;

        let warpf_1 = warp_factor(n, &L3 - &L2);
        let warpf_2 = warp_factor(n, &L1 - &L3);
        let warpf_3 = warp_factor(n, &L2 - &L1);

        let alpha_1 = &L1 * alpha;
        let alpha_2 = &L2 * alpha;
        let alpha_3 = &L3 * alpha;

        let warp_1 = blend_1
            .elemul(&warpf_1)
            .elemul(&(alpha_1.elemul(&alpha_1) + 1.));
        let warp_2 = blend_2
            .elemul(&warpf_2)
            .elemul(&(alpha_2.elemul(&alpha_2) + 1.));
        let warp_3 = blend_3
            .elemul(&warpf_3)
            .elemul(&(alpha_3.elemul(&alpha_3) + 1.));

        let x_res =
            x + &warp_1 * 1. + &warp_2 * (2. * PI / 3.).cos() + &warp_3 * (4. * PI / 3.).cos();
        let y_res =
            y + &warp_1 * 0. + &warp_2 * (2. * PI / 3.).sin() + &warp_3 * (4. * PI / 3.).sin();

        (x_res, y_res)
    }

    pub fn rs_to_ab(rs: &Vector<f64>, ss: &Vector<f64>) -> (Vector<f64>, Vector<f64>) {
        let a: Vector<f64> = rs.iter().zip(ss.iter()).map(|(&r, &s)| {
            if s != 1. { 2. * (r + 1.) / (1. - s) - 1. } else { -1. }
        }).collect();
        let b = ss.clone();
        (a, b)
    }
}

fn warp_factor(n_p: i32, gammas: Vector<f64>) -> Vector<f64> {
    let dist_gl = jacobi_polynomials::gauss_lobatto_points(n_p);
    let dist_eq: Vector<f64> = Vector::new(
        (0..n_p + 1)
            .into_iter()
            .map(|i| (i as f64) / (n_p as f64) * 2. - 1.)
            .collect::<Vec<f64>>(),
    );

    let v = vandermonde::vandermonde(&dist_eq, n_p);

    let n_r = gammas.size();

    let data: Vec<Vector<f64>> = (0..n_p + 1)
        .into_iter()
        .map(|i| jacobi_polynomials::jacobi(&gammas, 0, 0, i))
        .collect();

    let p_mat = Matrix::from_iter(data.iter().map(|vec| vec.data().as_slice()));

    let v_transpose_inv = v.transpose().inverse().expect("could not invert V'");
    let l_mat: Matrix<f64> = v_transpose_inv * p_mat;

    let warp = l_mat.transpose() * (dist_gl - dist_eq);

    let zero_f: Vector<f64> = Vector::from_fn(gammas.size(), |i: usize| {
        if { gammas[i].abs() < 1.0 - 1.0e-10 } {
            1.
        } else {
            0.
        }
    });

    let zero_f_gammas = zero_f.elemul(&gammas);
    let scaling_factor = zero_f_gammas.elemul(&zero_f_gammas) * -1. + 1.;

    warp.elediv(&scaling_factor) + warp.elemul(&(&zero_f - 1.))
}

#[cfg(test)]
mod tests {
    extern crate rulinalg;

    use super::warp_factor;
    use super::ReferenceElement;

    #[test]
    fn test_warp_factor() {
        let gammas = vector![-1., -0.5, 0., 0.5, 1.];
        let actual = warp_factor(10, gammas);
        assert_eq!(actual[1], -0.24330999741877501);
    }

    #[test]
    fn test_reference_element() {
        let re = ReferenceElement::legendre(10);
        assert_eq!(re.rs[13], -0.7309414433795846);
        assert_eq!(re.ss[13], -0.8960431364598923);
    }
}
