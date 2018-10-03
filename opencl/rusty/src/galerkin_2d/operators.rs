extern crate itertools;
extern crate rulinalg;

use functions::vandermonde::{grad_vandermonde_2d, vandermonde, vandermonde_2d};
use galerkin_2d::grid::Element;
use galerkin_2d::grid::XYTuple;
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::unknowns::Unknown;
use rulinalg::matrix::{BaseMatrix, BaseMatrixMut, Matrix};
use rulinalg::vector::Vector;
use galerkin_2d::grid::LocalMetric;
use std::fmt;

#[derive(Debug)]
pub struct Operators {
    // The Vandermonde matrix
    pub v: Matrix<f64>,

    // The D_r derivative matrix. D_r*V = V_r
    pub d_r: Matrix<f64>,
    // The D_s derivative matrix. D_s*V = V_s
    pub d_s: Matrix<f64>,

    // The matrix lifting the surface integral on the simplex edges to the
    // area integral over the simplex.
    pub lift: FaceLift,
}

#[derive(Debug)]
pub struct FaceLift {
    pub face1: Matrix<f64>,
    pub face2: Matrix<f64>,
    pub face3: Matrix<f64>,
}

impl fmt::Display for FaceLift {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        writeln!(f, "Face 1:")?;
        writeln!(f, "{}", self.face1)?;
        writeln!(f, "Face 2:")?;
        writeln!(f, "{}", self.face2)?;
        writeln!(f, "Face 3:")?;
        writeln!(f, "{}", self.face3)
    }
}

pub trait FaceLiftable: Unknown {
    fn lift_faces(
        face_lift: &FaceLift,
        face1: &Self::Line,
        face2: &Self::Line,
        face3: &Self::Line,
    ) -> Self;
}

pub fn assemble_operators(reference_element: &ReferenceElement) -> Operators {
    let n = reference_element.n as i32;
    let rs = &reference_element.rs;
    let ss = &reference_element.ss;

    let (a, b) = ReferenceElement::rs_to_ab(&rs, &ss);
    let v = vandermonde_2d(n, &a, &b);
    let v_inv = v
        .clone()
        .inverse()
        .expect("Non-invertible Vandermonde matrix");
    let (v_r, v_s) = grad_vandermonde_2d(n, &a, &b);

    let d_r = &v_r * &v_inv;
    let d_s = &v_s * &v_inv;

    let lift = assemble_lift(reference_element, &v);

    Operators { v, d_r, d_s, lift }
}

fn assemble_lift(reference_element: &ReferenceElement, v2d: &Matrix<f64>) -> FaceLift {
    let inv_mass_matrix = v2d * v2d.transpose();
    let n = reference_element.n as i32;
    let n_p = (n + 1) * (n + 2) / 2;
    let n_fp: usize = (n + 1) as usize;
    let epsilon = 1.0e-12;

    let ss = &reference_element.ss;
    let rs = &reference_element.rs;

    let mut face1: Matrix<f64> = Matrix::zeros(n_p as usize, n_fp as usize);
    let face1_r: Vector<f64> = rs.select(&reference_element.face1.as_slice());
    let v = vandermonde(&face1_r, n);
    let mass_face1 = (&v * &v.transpose()).inverse().expect("non-invertible");
    &reference_element
        .face1
        .iter()
        .enumerate()
        .for_each(|(j, &i)| {
            face1
                .row_mut(i as usize)
                .iter_mut()
                .zip(mass_face1.row(j).into_iter())
                .for_each(|(dest, x)| *dest = *x)
        });
    let lift_face_1 = &inv_mass_matrix * face1;

    let mut face2: Matrix<f64> = Matrix::zeros(n_p as usize, n_fp as usize);
    // Can use either r or s here; the important thing is that they are distributed in the
    // same way along the diagonal edge.
    let face2_r: Vector<f64> = rs.select(&reference_element.face2.as_slice());
    let v = vandermonde(&face2_r, n);
    let mass_face2 = (&v * &v.transpose()).inverse().expect("non-invertible");
    &reference_element
        .face2
        .iter()
        .enumerate()
        .for_each(|(j, &i)| {
            face2
                .row_mut(i as usize)
                .iter_mut()
                .zip(mass_face1.row(j).into_iter())
                .for_each(|(dest, x)| *dest = *x)
        });
    let lift_face_2 = &inv_mass_matrix * face2;

    let mut face3: Matrix<f64> = Matrix::zeros(n_p as usize, n_fp as usize);
    let face3_s: Vector<f64> = ss.select(&reference_element.face3.as_slice());
    let v = vandermonde(&face3_s, n_p);
    let mass_face3 = (&v * &v.transpose()).inverse().expect("non-invertible");
    &reference_element
        .face3
        .iter()
        .enumerate()
        .for_each(|(j, &i)| {
            face3
                .row_mut(i as usize)
                .iter_mut()
                .zip(mass_face1.row(j).into_iter())
                .for_each(|(dest, x)| *dest = *x)
        });
    let lift_face_3 = &inv_mass_matrix * face3;

    FaceLift {
        face1: lift_face_1,
        face2: lift_face_2,
        face3: lift_face_3,
    }
}

pub fn grad(
    u: &Vector<f64>,
    operators: &Operators,
    local_metric: &LocalMetric,
) -> XYTuple<Vector<f64>> {
    let u_r = &operators.d_r * u;
    let u_s = &operators.d_s * u;
    let u_x = local_metric.r_x.elemul(&u_r) + local_metric.s_x.elemul(&u_s);
    let u_y = local_metric.r_y.elemul(&u_r) + local_metric.s_y.elemul(&u_s);
    XYTuple { x: u_x, y: u_y }
}

pub fn curl_2d(
    u_x: &Vector<f64>,
    u_y: &Vector<f64>,
    operators: &Operators,
    local_metric: &LocalMetric,
) -> Vector<f64> {
    let u_xr = &operators.d_r * u_x;
    let u_xs = &operators.d_s * u_x;
    let u_yr = &operators.d_r * u_y;
    let u_ys = &operators.d_s * u_y;
    let v_z = u_yr.elemul(&local_metric.r_x) + u_ys.elemul(&local_metric.s_x)
        - u_xr.elemul(&local_metric.r_y) - u_xs.elemul(&local_metric.s_y);
    v_z
}
