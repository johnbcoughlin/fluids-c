extern crate rulinalg;

use self::rulinalg::matrix::Matrix;
use self::rulinalg::vector::Vector;
use functions::jacobi_polynomials::{
    grad_jacobi, grad_simplex_2d_polynomials, jacobi, simplex_2d_polynomial,
};
use rulinalg::matrix::BaseMatrixMut;

pub fn vandermonde(rs: &Vector<f64>, n: i32) -> Matrix<f64> {
    let mut v = Matrix::zeros(rs.size(), (n + 1) as usize);
    for j in 0..n + 1 {
        let mut column = v.col_mut(j as usize);
        let vals = jacobi(rs, 0, 0, j);
        for (dest, src) in column.iter_mut().zip(vals.into_iter()) {
            *dest = src;
        }
    }
    v
}

pub fn grad_vandermonde(rs: &Vector<f64>, n: i32) -> Matrix<f64> {
    let mut v = Matrix::zeros(rs.size(), (n + 1) as usize);
    for j in 0..n + 1 {
        let mut column = v.col_mut(j as usize);
        let vals = grad_jacobi(rs, 0, 0, j);
        for (dest, src) in column.iter_mut().zip(vals.into_iter()) {
            *dest = src;
        }
    }
    v
}

pub fn vandermonde_2d(n: i32, a: &Vector<f64>, b: &Vector<f64>) -> Matrix<f64> {
    assert_eq!(a.size(), b.size());
    let n_cols = (n as usize + 1) * (n as usize + 2) / 2;
    let mut v = Matrix::zeros(a.size(), n_cols);

    let mut s_k = 0;
    (0..n + 1).for_each(|i| {
        (0..n + 1 - i).for_each(|j| {
            let mut col = v.col_mut(s_k as usize);
            let simplex = simplex_2d_polynomial(a, b, i, j);
            simplex
                .into_iter()
                .zip(col.iter_mut())
                .for_each(|(x, dest)| *dest = x);
            s_k = s_k + 1;
        })
    });
    v
}

pub fn grad_vandermonde_2d(n: i32, a: &Vector<f64>, b: &Vector<f64>) -> (Matrix<f64>, Matrix<f64>) {
    assert_eq!(a.size(), b.size());
    let n_cols = (n as usize + 1) * (n as usize + 2) / 2;

    let mut v_r = Matrix::zeros(a.size(), n_cols);
    let mut v_s = Matrix::zeros(a.size(), n_cols);

    let mut s_k = 0;
    (0..n + 1).for_each(|i| {
        (0..n + 1 - i).for_each(|j| {
            let mut col_r = v_r.col_mut(s_k as usize);
            let mut col_s = v_s.col_mut(s_k as usize);
            let (simplex_r, simplex_s) = grad_simplex_2d_polynomials(a, b, i, j);
            simplex_r
                .into_iter()
                .zip(col_r.iter_mut())
                .for_each(|(x, dest)| *dest = x);
            simplex_s
                .into_iter()
                .zip(col_s.iter_mut())
                .for_each(|(x, dest)| *dest = x);
            s_k = s_k + 1;
        })
    });
    (v_r, v_s)
}

#[cfg(test)]
mod tests {
    use functions::jacobi_polynomials::grad_legendre_roots;
    use functions::vandermonde::{grad_vandermonde, vandermonde, vandermonde_2d};
    use std::ops::Index;

    #[test]
    fn test_vandermonde() {
        let rs = grad_legendre_roots(5);
        let v = vandermonde(&rs, 5);
        assert!((0.7071 - v[[0, 0]]).abs() < 0.001);
        assert!((-1.2247 - v[[0, 1]]).abs() < 0.001);
        assert!((-1.8708 - v[[0, 3]]).abs() < 0.001);
    }

    #[test]
    fn test_grad_vandermonde() {
        let rs = grad_legendre_roots(5);
        let v = grad_vandermonde(&rs, 5);
        assert!((0.0000 - v[[0, 0]]).abs() < 0.001);
        assert!((1.2247 - v[[0, 1]]).abs() < 0.001);
        assert!((11.2250 - v[[0, 3]]).abs() < 0.001);
    }

    #[test]
    fn test_vandermonde_2d() {
        let a = vector![
            -1.,
            -0.733782082764989,
            0.112279732256502,
            2.59621764561432,
            14.0252847048942,
            -1.
        ];
        let b = vector![
            -1.,
            -0.765055323929465,
            -0.285231516480645,
            0.285231516480645,
            0.765055323929465,
            1.
        ];
        let v = vandermonde_2d(5, &a, &b);
        assert_eq!(*v.index([1, 2]), 0.24276745596088087);
        assert_eq!(*v.index([4, 11]), 11.132172517758661);
    }
}
