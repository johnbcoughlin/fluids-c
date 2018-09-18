extern crate rulinalg;

use functions::vandermonde::{grad_vandermonde, vandermonde};
use galerkin_1d::grid::ReferenceElement;
use galerkin_1d::unknowns::Unknown;
use rulinalg::matrix::{BaseMatrix, Matrix};

pub struct Operators {
    // The Vandermonde matrix
    pub v: Matrix<f64>,

    // The D_r derivative matrix
    pub d_r: Matrix<f64>,

    // The matrix lifting [a, b] to [a, ..., 0, ..., b] with length n_p, followed by the inverse
    // mass matrix.
    pub lift: Matrix<f64>,
}

pub fn assemble_operators<U>(reference_element: &ReferenceElement) -> Operators
where
    U: Unknown,
{
    let n_p = reference_element.n_p;
    let rs = &reference_element.rs;

    let v = vandermonde(&rs, n_p);
    let v_inv = v
        .clone()
        .inverse()
        .expect("Non-invertible Vandermonde matrix");
    let v_r = grad_vandermonde(&rs, n_p);
    let d_r = &v_r * &v_inv;

    let mut vals: Vec<f64> = vec![0.0; (n_p as usize + 1) * 2];
    vals[0] = 1.0;
    vals[2 * n_p as usize + 1] = 1.0;
    let e_mat = Matrix::new(n_p as usize + 1, 2, vals);
    let lift = &v * &(v.transpose() * e_mat);

    Operators { v, d_r, lift }
}
