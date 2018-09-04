extern crate rulinalg;
extern crate itertools;

use rulinalg::matrix::{Matrix, BaseMatrix, BaseMatrixMut};
use rulinalg::vector::Vector;
use galerkin_2d::reference_element::ReferenceElement;
use functions::vandermonde::{vandermonde, vandermonde_2d, grad_vandermonde_2d};

pub struct Operators {
    // The Vandermonde matrix
    pub v: Matrix<f64>,

    // The D_r derivative matrix. D_r*V = V_r
    pub d_r: Matrix<f64>,
    // The D_s derivative matrix. D_s*V = V_s
    pub d_s: Matrix<f64>,

    // The matrix lifting the surface integral on the simplex edges to the
    // area integral over the simplex.
    pub lift: Matrix<f64>,
}

pub fn assemble_operators(reference_element: &ReferenceElement) -> Operators {
    let n_p = reference_element.n_p;
    let rs = &reference_element.rs;
    let ss = &reference_element.ss;

    let (a, b) = ReferenceElement::rs_to_ab(&rs, &ss);
    let v = vandermonde_2d(n_p, &a, &b);
    let v_inv = v.clone().inverse().expect("Non-invertible Vandermonde matrix");
    let (v_r, v_s) = grad_vandermonde_2d(n_p, &a, &b);

    let d_r = &v_r * &v_inv;
    let d_s = &v_s * &v_inv;

    let lift = assemble_lift(reference_element);

    Operators {
        v,
        d_r,
        d_s,
        lift,
    }
}

fn assemble_lift(reference_element: &ReferenceElement) -> Matrix<f64> {
    let n_p = reference_element.n_p;
    let n = (n_p + 1) * (n_p + 2) / 2;
    let n_fp: usize = (n_p + 1) as usize;
    let epsilon = 1.0e-12;

    let mut E: Matrix<f64> = Matrix::zeros(n as usize, 3 * n_fp as usize);

    let ss = &reference_element.ss;
    let rs = &reference_element.rs;
    let face1_r: Vector<f64> = rs.select(&reference_element.face1.as_slice());
    let v = vandermonde(&face1_r, n_p);
    let mass_face1 = (&v * &v.transpose()).inverse().expect("non-invertible");
    &reference_element.face1.iter().enumerate()
        .for_each(|(j, &i)| E.row_mut(i as usize).sub_slice_mut([0, 0], 1, n_fp as usize).iter_mut()
            .zip(mass_face1.row(j).into_iter())
            .for_each(|(dest, x)| *dest = *x));

    // Can use either r or s here; the important thing is that they are distributed in the
    // same way along the diagonal edge.
    let face2_r: Vector<f64> = rs.select(&reference_element.face2.as_slice());
    let v = vandermonde(&face2_r, n_p);
    let mass_face2 = (&v * &v.transpose()).inverse().expect("non-invertible");
    &reference_element.face2.iter().enumerate()
        .for_each(|(j, &i)| E.row_mut(i as usize).sub_slice_mut([0, n_fp as usize], 1, n_fp as usize).iter_mut()
            .zip(mass_face1.row(j).into_iter())
            .for_each(|(dest, x)| *dest = *x));

    let face3_s: Vector<f64> = ss.select(&reference_element.face3.as_slice());
    let v = vandermonde(&face3_s, n_p);
    let mass_face3 = (&v * &v.transpose()).inverse().expect("non-invertible");
    &reference_element.face3.iter().enumerate()
        .for_each(|(j, &i)| E.row_mut(i as usize).sub_slice_mut([0, 2 * n_fp], 1, n_fp as usize).iter_mut()
            .zip(mass_face1.row(j).into_iter())
            .for_each(|(dest, x)| *dest = *x));

    E
}

