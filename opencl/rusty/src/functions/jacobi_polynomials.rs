extern crate arrayfire;
extern crate num;

use num::traits::real::Real;
use self::arrayfire::{Array, Scalar, Dim4, DType, row, print, constant_t};
use functions::gamma::gamma;
use functions::gamma::GammaFn;

pub fn jacobi(xs: Array, alpha: i32, beta: i32, n: i32) -> Array {
    let xdims = xs.dims();
    let dim = xdims.get();
    assert!(dim[1] == 1 && dim[2] == 1 && dim[3] == 1, "xs array must be one-dimensional");

    let alphaf = alpha as f32;
    let betaf = beta as f32;

    // initial values
    // See NUDG p. 446
    let gamma_0 = 2.0.powi(alpha + beta + 1) / (alphaf + betaf + 1.) *
        (alphaf + 1.).gamma() * (betaf + 1.).gamma() / (alphaf + betaf + 1.).gamma();
    let p_0 = arrayfire::constant_t(Scalar::F32(1.0 / gamma_0.sqrt() as f32),
                                    Dim4::new(&[dim[0], 1, 1, 1]), DType::F32);
    if n == 0 {
        return p_0;
    }

    let gamma_1 = (alphaf + 1.) * (betaf + 1.) / (alphaf + betaf + 3.) * gamma_0;
    let p_1 = (xs * ((alphaf + betaf + 2.) / 2.) + (alphaf - betaf) / 2.) / gamma_1.sqrt();
    if n == 1 {
        return p_1
    }


    let a_old = 2. / (2. + alphaf + betaf) *
        ((alphaf + 1.) * (betaf + 1.) / (alphaf + betaf + 3.)).sqrt();

    let mut p_i_minus_1 = p_0;
    let mut p_i = p_1;

    for i in 1..n-1 {
        let i = i as f32;
        let h1 = 2. * (i + alphaf + betaf);
        let a_new = 2. / (h1 + 2.) * ((i + 1.) * (i + 1. + alphaf + betaf) * (i + 1. + alphaf) *
            (i + 1. + betaf) / (h1 + 1.) / (h1 + 3.)).sqrt();
        let b_new = - (alphaf * alphaf - betaf * betaf) / h1 / (h1 + 2.);
        let p_i_plus_1 = (-p_i_minus_1 * a_old + (xs - b_new) * p_i) * (1. / a_new);
        let p_i_minus_1 = p_i;
        let p_i = p_i_plus_1;
        let a_old = a_new;
    }
    return p_i;
}


#[cfg(test)]
mod tests {
    extern crate arrayfire;

    use functions::jacobi_polynomials::jacobi;
    use self::arrayfire::{Array, Dim4, print};

    #[test]
    fn test() {
        let xs: [f32; 4] = [0.5, 1.0, 1.5, 2.0];
        let p = jacobi(Array::new(&xs, Dim4::new(&[4, 1, 1, 1])), 1, 1, 3);
        print(&p);
    }
}
