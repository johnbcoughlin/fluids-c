extern crate arrayfire;

use self::arrayfire::{Array, Dim4, DType, Seq, Scalar,
                      inverse, matmul, constant_t, assign_seq, print, MatProp};
use galerkin_1d::grid::{Element, ReferenceElement, Grid, Face};
use functions::vandermonde::{vandermonde, grad_vandermonde};

pub struct Operators {
    // The linear flux operator
    pub a: f32,

    // The Lax-Friedrichs flux parameter
    pub alpha: f32,

    // The Vandermonde matrix
    pub v: Array,

    // The D_r derivative matrix
    pub d_r: Array,

    // The matrix lifting [a, b] to [a, ..., 0, ..., b] with length n_p, followed by the inverse
    // mass matrix.
    pub lift: Array,
}

pub fn assemble_operators(a: f32, grid: &Grid, reference_element: &ReferenceElement) -> Operators {
    let n_p = reference_element.n_p;
    let rs = &reference_element.rs;

    let v = vandermonde(&rs, n_p);
    let v_r = grad_vandermonde(&rs, n_p);
    let d_r = matmul(&v_r, &inverse(&v, MatProp::NONE), MatProp::NONE, MatProp::NONE);

    let mut vals: Vec<f32> = vec![0.0 as f32; (n_p as usize + 1) * 2];
    vals[0] = 1.0;
    vals[2 * n_p as usize + 1] = 1.0;
    let e_mat = Array::new(vals.as_slice(), Dim4::new(&[n_p as u64 + 1, 2, 1, 1]));

    let lift = matmul(&v, &matmul(&v, &e_mat, MatProp::TRANS, MatProp::NONE),
                      MatProp::NONE, MatProp::NONE);

    Operators {
        a,
        alpha: 1.0,
        v,
        d_r,
        lift,
    }
}
