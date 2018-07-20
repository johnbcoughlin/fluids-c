extern crate rulinalg;

use self::rulinalg::vector::Vector;
use self::rulinalg::matrix::Matrix;
use rulinalg::matrix::BaseMatrixMut;
use functions::jacobi_polynomials::{jacobi, grad_jacobi};

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

#[cfg(test)]
mod tests {
    extern crate arrayfire;

    use functions::jacobi_polynomials::grad_legendre_roots;
    use functions::vandermonde::{vandermonde, grad_vandermonde};

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
}