extern crate num;

use self::num::complex::Complex64;
use std::f64::consts;

const P: [f64; 8] = [
    676.5203681218851,
    -1259.1392167224028,
    771.32342877765313,
    -176.61502916214059,
    12.507343278686905,
    -0.13857109526572012,
    9.9843695780195716e-6,
    1.5056327351493116e-7,
];

// Using the Lanczos approximation to the Gamma Function
// https://en.wikipedia.org/wiki/Lanczos_approximation
pub fn gamma(z: Complex64) -> Complex64 {
    if z.re < 0.5 {
        let y = consts::PI / ((z * consts::PI).sin() * gamma(-z + 1.0));
        return y;
    } else {
        let z = z - 1.0;
        let mut x = Complex64::new(0.99999999999980993, 0.0);
        for i in 0..P.len() {
            let z_inv = (z + ((i + 1) as f64)).inv();
            x = x + z_inv.scale(P[i]);
        }
        let t = z + (P.len() as f64) - 0.5;
        let y = (2.0 * consts::PI).sqrt() * t.powc(z + Complex64::from(0.5)) * (-t).exp() * x;
        return y;
    }
}

impl GammaFn for f32 {
    type Output = f32;

    fn gamma(&self) -> f32 {
        gamma(Complex64::from(*self as f64)).re as f32
    }
}

impl GammaFn for f64 {
    type Output = f64;

    fn gamma(&self) -> f64 {
        gamma(Complex64::from(self)).re
    }
}

pub trait GammaFn {
    type Output;

    fn gamma(&self) -> Self::Output;
}

#[cfg(test)]
mod tests {
    use super::num::complex::Complex64;
    use functions::gamma::gamma;
    use std::f64::consts;

    #[test]
    fn test_min() {
        test_gamma(0.5, consts::PI.sqrt());
    }

    #[test]
    fn test_1() {
        test_gamma(1.0, 1.0);
    }

    #[test]
    fn test_3() {
        test_gamma(3.0, 2.0);
    }

    #[test]
    fn test_5() {
        test_gamma(5.0, 24.0);
    }

    #[test]
    fn test_8() {
        test_gamma(8.0, 5040.0);
    }

    fn test_gamma(arg: f64, expected: f64) {
        let actual = gamma(Complex64::from(arg));
        assert_eq!(actual.im, 0.0);
        assert!((actual.re - expected).abs() < 1e-10);
    }
}
