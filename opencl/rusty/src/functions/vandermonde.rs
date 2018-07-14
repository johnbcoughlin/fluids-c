extern crate arrayfire;

use self::arrayfire::{Array, Scalar, Dim4, DType, print, constant_t, set_col};
use functions::jacobi_polynomials::{jacobi, grad_jacobi};

pub fn vandermonde(rs: &Array, n: i32) -> Array {
    let v = constant_t(Scalar::F32(0.0),
                       Dim4::new(&[(&rs.dims()).get()[0], (n + 1) as u64, 1, 1]),
                       DType::F32);
    for j in 0..n + 1 {
        set_col(&v, &jacobi(rs, 0, 0, j), j as u64);
    }
    v
}

pub fn grad_vandermonde(rs: &Array, n: i32) -> Array {
    let v = constant_t(Scalar::F32(0.0),
                       Dim4::new(&[(&rs.dims()).get()[0], (n + 1) as u64, 1, 1]),
                       DType::F32);
    for j in 0..n + 1 {
        set_col(&v, &grad_jacobi(rs, 0, 0, j), j as u64);
    }
    v
}

#[cfg(test)]
mod tests {
    extern crate arrayfire;

    use self::arrayfire::{Array, Scalar, Dim4, DType, print, constant_t, set_col};
    use functions::jacobi_polynomials::grad_legendre_roots;
    use functions::vandermonde::{vandermonde, grad_vandermonde};

    #[test]
    fn test_vandermonde() {
        let rs = grad_legendre_roots(5);
        let v = vandermonde(&rs, 5);
        v.eval();
        let mut h: [f32; 36] = [0.0; 36];
        v.host(&mut h);
        assert!((0.7071 - h[0]).abs() < 0.001);
        assert!((-1.2247 - h[6]).abs() < 0.001);
        assert!((-1.8708 - h[18]).abs() < 0.001);
    }

    #[test]
    fn test_grad_vandermonde() {
        let rs = grad_legendre_roots(5);
        let v = grad_vandermonde(&rs, 5);
        v.eval();
        let mut h: [f32; 36] = [0.0; 36];
        print(&v);
        v.host(&mut h);
        assert!((0.0000 - h[0]).abs() < 0.001);
        assert!((1.2247 - h[6]).abs() < 0.001);
        assert!((11.2250 - h[18]).abs() < 0.001);
    }
}