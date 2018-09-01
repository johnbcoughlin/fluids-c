extern crate rulinalg;
extern crate typenum;
extern crate generic_array as ga;

use self::ga::ArrayLength;
use self::rulinalg::vector::Vector as RaVector;
use self::rulinalg::matrix::Matrix;
use rulinalg::matrix::BaseMatrixMut;
use functions::jacobi_polynomials::{jacobi, grad_jacobi};
use matrices::vector_ops::Vector;
use self::typenum::uint::Unsigned;

pub fn vandermonde<N>(rs: &Vector<N>, n: i32) -> Matrix<f64>
    where
        N: Unsigned + ArrayLength<f64>
{
    let mut v = Matrix::zeros(rs.size(), (n + 1) as usize);
    for j in 0..n + 1 {
        let mut column = v.col_mut(j as usize);
        let vals = jacobi::<N>(rs, 0, 0, j);
        for (dest, src) in column.iter_mut().zip(vals.into_iter()) {
            *dest = src;
        }
    }
    v
}

pub fn grad_vandermonde<N>(rs: &RaVector<f64>, n: i32) -> Matrix<f64>
    where
        N: Unsigned + ArrayLength<f64>
{
    let mut v = Matrix::zeros(rs.size(), (n + 1) as usize);
    for j in 0..n + 1 {
        let mut column = v.col_mut(j as usize);
        let vals = grad_jacobi::<N>(rs, 0, 0, j);
        for (dest, src) in column.iter_mut().zip(vals.into_iter()) {
            *dest = src;
        }
    }
    v
}

pub fn vandermonde_2d(n: i32, a: RaVector<f64>, b: RaVector<f64>) {
    assert_eq!(a.size(), b.size());
    let n_cols = (n as usize + 1) * (n as usize + 2) / 2;
//    let mut v = Matrix::zeros(a.size(), n_cols);

    let s_k = 0;
    (0..n + 1).for_each(|i| {
        (0..n + 1 - i).for_each(|j| {
//            let mut row = v.row_mut(i as usize);
        })
    })
}

#[cfg(test)]
mod tests {
    extern crate typenum;

    use functions::jacobi_polynomials::grad_legendre_roots;
    use functions::vandermonde::{vandermonde, grad_vandermonde};
    use matrices::vector_ops::Vector;
    use self::typenum::{U0, U1, U2, U3, U4};

    #[test]
    fn test_vandermonde() {
        let rs = grad_legendre_roots(5);
        let v = vandermonde::<U4>(&Vector::from_rulinalg(&rs), 5);
        assert!((0.7071 - v[[0, 0]]).abs() < 0.001);
        assert!((-1.2247 - v[[0, 1]]).abs() < 0.001);
        assert!((-1.8708 - v[[0, 3]]).abs() < 0.001);
    }

    #[test]
    fn test_grad_vandermonde() {
        let rs = grad_legendre_roots(5);
        assert_eq!(rs.size(), 4);
        let v = grad_vandermonde::<U4>(&rs, 5);
        assert!((0.0000 - v[[0, 0]]).abs() < 0.001);
        assert!((1.2247 - v[[0, 1]]).abs() < 0.001);
        assert!((11.2250 - v[[0, 3]]).abs() < 0.001);
    }
}