extern crate accelerate_src;
extern crate lapack;
extern crate num;
extern crate rulinalg;

use self::lapack::*;
use self::num::traits::real::Real;
use self::rulinalg::vector::Vector;
use functions::gamma::GammaFn;

pub fn jacobi(xs: &Vector<f64>, alpha: i32, beta: i32, n: i32) -> Vector<f64> {
    let alphaf = alpha as f64;
    let betaf = beta as f64;

    // initial values
    // See NUDG p. 446
    let gamma_0 = 2.0.powi(alpha + beta + 1) / (alphaf + betaf + 1.)
        * (alphaf + 1.).gamma()
        * (betaf + 1.).gamma() / (alphaf + betaf + 1.).gamma();
    let p_0 = Vector::ones(xs.size()) * (1.0 / gamma_0.sqrt());
    if n == 0 {
        return p_0;
    }

    let gamma_1 = (alphaf + 1.) * (betaf + 1.) / (alphaf + betaf + 3.) * gamma_0;
    let p_1 = (xs * ((alphaf + betaf + 2.) / 2.) + (alphaf - betaf) / 2.) / gamma_1.sqrt();
    if n == 1 {
        return p_1;
    }

    let mut a_old =
        2. / (2. + alphaf + betaf) * ((alphaf + 1.) * (betaf + 1.) / (alphaf + betaf + 3.)).sqrt();

    let mut p_i_minus_1 = p_0;
    let mut p_i = p_1;

    for i in 1..n {
        let i = i as f64;
        let h1 = 2. * i + alphaf + betaf;
        let a_new = 2. / (h1 + 2.)
            * ((i + 1.) * (i + 1. + alphaf + betaf) * (i + 1. + alphaf) * (i + 1. + betaf)
                / (h1 + 1.)
                / (h1 + 3.))
                .sqrt();
        let b_new = -(alphaf * alphaf - betaf * betaf) / h1 / (h1 + 2.);
        let mut p_i_plus_1 = (-(p_i_minus_1) * a_old + (xs - b_new).elemul(&p_i)) * (1. / a_new);
        p_i_minus_1 = p_i;
        p_i = p_i_plus_1;
        a_old = a_new;
    }
    return p_i;
}

pub fn grad_jacobi(xs: &Vector<f64>, alpha: i32, beta: i32, n: i32) -> Vector<f64> {
    if n == 0 {
        return Vector::zeros(xs.size());
    }
    let alphaf = alpha as f64;
    let betaf = beta as f64;
    let nf = n as f64;
    let factor: f64 = (nf * (nf + alphaf + betaf + 1.)).sqrt();
    let j = jacobi(xs, alpha + 1, beta + 1, n - 1);
    return j * factor;
}

// The Legendre polynomials are P_n(0, 0), the Jacobi polynomials with alpha = beta = 0.
// This function returns the zeros of (1 - x^2)P_n'(0, 0), i.e. the zeros of the derivative
// of the nth Legendre polynomial, plus -1 and 1.
pub fn grad_legendre_roots(n: i32) -> Vector<f64> {
    let n = n - 1;
    let mut diag = vec![0.0; n as usize];
    let mut subdiag: Vec<f64> = (2..n + 1)
        .map(|i| {
            let i = i as f64;
            let num = (i + 1.) / i;
            let denom = (2. * i - 1.) / (i - 1.) * (i * 2. + 1.) / (i);
            (num / denom).sqrt()
        })
        .collect();
    let mut z = vec![];
    let mut work = vec![0.0; 4 * n as usize];
    let mut info = 0;

    unsafe {
        dstev(
            b'N',
            n,
            &mut diag,
            &mut subdiag,
            &mut z,
            1,
            &mut work,
            &mut info,
        );
    }

    return Vector::new(diag);
}

pub fn gauss_lobatto_points(n: i32) -> Vector<f64> {
    let mut rs = vec![-1.];
    let roots = grad_legendre_roots(n);
    for r in roots.into_iter() {
        rs.push(r);
    }
    rs.push(1.);
    Vector::new(rs)
}

pub fn simplex_2d_polynomial(a: &Vector<f64>, b: &Vector<f64>, i: i32, j: i32) -> Vector<f64> {
    let h1 = jacobi(a, 0, 0, i);
    let h2 = jacobi(b, 2 * i + 1, 0, j);
    let base: Vector<f64> = -b + 1.;
    let mut x = Vector::ones(base.size());
    (0..i).for_each(|_| {
        x = x.elemul(&base);
    });
    let result = (h1.elemul(&h2) * 2.0_f64.sqrt()).elemul(&x);
    result
}

/**
 * GradSimplex2DP.m
 * Returns the r- and s-derivatives of the modal basis functions at the points (a, b),
 * on the 2D reference simplex.
 *
 * Hesthaven and Warburton, p. 184
 */
pub fn grad_simplex_2d_polynomials(
    a: &Vector<f64>,
    b: &Vector<f64>,
    i: i32,
    j: i32,
) -> (Vector<f64>, Vector<f64>) {
    let fa = jacobi(a, 0, 0, i);
    let d_fa = grad_jacobi(a, 0, 0, i);
    let gb = jacobi(b, 2 * i + 1, 0, j);
    let d_gb = grad_jacobi(b, 2 * i + 1, 0, j);

    // r-derivative
    let mut dmode_dr = d_fa.elemul(&gb);
    if i > 0 {
        dmode_dr = dmode_dr
            .into_iter()
            .zip(b.iter())
            .map(|(x, b)| x * ((0.5 * (1. - b)).powi(i - 1)))
            .collect();
    }

    // s-derivative
    let mut dmode_ds = ((a + 1.) * 0.5).elemul(&gb).elemul(&d_fa);
    if i > 0 {
        dmode_ds = dmode_ds
            .iter()
            .zip(b.iter())
            .map(|(x, b)| *x * ((0.5 * (1. - *b)).powi(i - 1)))
            .collect();
    }
    let mut tmp: Vector<f64> = d_gb
        .iter()
        .zip(b.iter())
        .map(|(x, b)| *x * ((0.5 * (1. - *b)).powi(i)))
        .collect();
    if i > 0 {
        let diff: Vector<f64> = gb
            .iter()
            .zip(b.iter())
            .map(|(x, b)| *x * ((0.5 * (1. - *b)).powi(i - 1)))
            .collect::<Vector<f64>>();
        tmp = tmp - &(diff * 0.5 * (i as f64));
    }
    dmode_ds = dmode_ds + fa.elemul(&tmp);

    dmode_dr = dmode_dr * 2.0_f64.powf(i as f64 + 0.5);
    dmode_ds = dmode_ds * 2.0_f64.powf(i as f64 + 0.5);

    (dmode_dr, dmode_ds)
}

#[cfg(test)]
mod tests {
    extern crate rulinalg;

    use functions::jacobi_polynomials::{
        grad_jacobi, grad_legendre_roots, grad_simplex_2d_polynomials, jacobi,
    };

    #[test]
    fn test_jacobi_0() {
        test_jacobi_val(1.0, 1, 1, 0, 0.86600);
        test_jacobi_val(100.0, 1, 1, 0, 0.86600);

        test_jacobi_val(1.0, 2, 2, 0, 0.968245);
    }

    #[test]
    fn test_jacobi_1() {
        test_jacobi_val(1.0, 1, 1, 1, 1.9365);
        test_jacobi_val(2.0, 1, 1, 1, 3.8730);

        test_jacobi_val(1.0, 2, 2, 1, 2.5617);
    }

    #[test]
    fn test_jacobi_3() {
        test_jacobi_val(0.5, 1, 1, 3, -0.7412);
        test_jacobi_val(2.0, 1, 1, 3, 59.2927);

        test_jacobi_val(1.0, 2, 2, 3, 8.4963);
    }

    fn test_jacobi_val(x: f64, alpha: i32, beta: i32, n: i32, expected_value: f64) {
        let xs = vector![x];
        let p = jacobi(&xs, alpha, beta, n);
        assert!((p[0] - expected_value).abs() < 0.0001);
    }

    #[test]
    fn test_grad_jacobi_0() {
        test_grad_jacobi_val(0.0, 1, 1, 0, 0.0);
        test_grad_jacobi_val(100.0, 1, 1, 0, 0.0);
    }

    #[test]
    fn test_grad_jacobi_1() {
        test_grad_jacobi_val(0.0, 1, 1, 1, 1.9365);
        test_grad_jacobi_val(1.0, 1, 1, 1, 1.9365);
    }

    #[test]
    fn test_grad_jacobi_3() {
        test_grad_jacobi_val(1.0, 1, 1, 3, 21.3454);
        test_grad_jacobi_val(1.0, 2, 2, 3, 33.9853);
    }

    fn test_grad_jacobi_val(x: f64, alpha: i32, beta: i32, n: i32, expected_value: f64) {
        let xs = vector![x];
        let p = grad_jacobi(&xs, alpha, beta, n);
        assert!((p[0] - expected_value).abs() < 0.0001);
    }

    #[test]
    fn test() {
        let roots = grad_legendre_roots(5);
        for &x in roots.iter() {
            assert!((l_prime_5(x).abs() < 1e-5));
        }
    }

    fn l_prime_5(x: f64) -> f64 {
        (5. * x.powi(4) * 63. - 3. * x.powi(2) * 70. + 15.) / 8.
    }

    #[test]
    fn test_grad_simplex_2d() {
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
        let (dr, ds) = grad_simplex_2d_polynomials(&a, &b, 1, 1);
        assert_eq!(dr[1], -0.8753380411610013);
        assert_eq!(ds[4], 12.357277800120185);
    }
}
