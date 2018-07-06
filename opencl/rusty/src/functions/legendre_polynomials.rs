extern crate num;
extern crate lapack;
extern crate accelerate_src;

use self::lapack::*;
use num::traits::real::Real;

// n is the order of the polynomial
// the method here is described at
// https://math.stackexchange.com/questions/12160/roots-of-legendre-polynomial
pub fn roots(n: i32) {
    let n = 5;
    let mut diag = vec![0.0; n as usize];
    let mut subdiag = vec![1.0 / 3.0.sqrt(), 2.0 / 15.0.sqrt(), 3.0 / 35.0.sqrt(), 4.0 / 63.0.sqrt()];
    let mut z = vec![];
    let mut w = vec![0.0; n as usize];
    let mut work = vec![0.0; 4 * n as usize];
    let lwork = 4 * n;
    let mut info = 0;

    unsafe {
        dstev(b'N', n, &mut diag, &mut subdiag, &mut z, 1, &mut work, &mut info);
    }

    println!("{:?}", diag);

    for i in 0..diag.len() {
        let x = diag[i];
        println!("{}", L5(x));
    }
}

fn L5(x: f64) -> f64 {
    (63.0 * x.powi(5) - 70.0 * x.powi(3) + 15.0 * x) / 8.0
}

#[cfg(test)]
mod tests {
    use functions::legendre_polynomials::*;

    #[test]
    fn test() {
        roots(2);
    }
}