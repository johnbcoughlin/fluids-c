extern crate num;
extern crate lapack;
extern crate accelerate_src;

use self::lapack::*;
use num::traits::real::Real;

// n is the order of the polynomial
// the method here is described at
// https://math.stackexchange.com/questions/12160/roots-of-legendre-polynomial
pub fn roots(n: i32) -> Vec<f64> {
    let n = n - 1;
    let mut diag = vec![0.0; n as usize];
    let mut subdiag: Vec<f64> = (2..n+1).map(|i| {
        let i = i as f64;
        let num = (i + 1.) / i;
        let denom = (2. * i - 1.) / (i - 1.) * (i * 2. + 1.) / (i);
        (num / denom).sqrt()
    }).collect();
    println!("{:?}", subdiag);
    let mut z = vec![];
    let mut w = vec![0.0; n as usize];
    let mut work = vec![0.0; 4 * n as usize];
    let lwork = 4 * n;
    let mut info = 0;

    unsafe {
        dstev(b'N', n, &mut diag, &mut subdiag, &mut z, 1, &mut work, &mut info);
    }

    diag
}

#[cfg(test)]
mod tests {
    use functions::legendre_polynomials::*;

    #[test]
    fn test() {
        let roots = roots(5);
        println!("{:?}", roots);
        for x in roots {
            println!("{}", l_prime_5(x));
            assert!((l_prime_5(x).abs() < 1e-10));
        }
    }

    fn l_prime_5(x: f64) -> f64 {
        (5. * x.powi(4) * 63. - 3. * x.powi(2) * 70. + 15.) / 8.
    }
}