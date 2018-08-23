extern crate rulinalg;

use self::rulinalg::vector::Vector;
use functions::jacobi_polynomials;
use functions::vandermonde;

pub struct ReferencePoint {
    r: f64,
    s: f64,
}

pub struct ReferenceElement {
    pub n_p: i32,

    pub rs: Vec<ReferencePoint>,
}

impl ReferenceElement {
    pub fn legendre(n_p: i32) {

    }
}

fn warp_factor(n_p: i32, gammas: Vector<f64>) -> Vector<f64> {
    let dist_gl = jacobi_polynomials::gauss_lobatto_points(n_p);
    let dist_eq = Vector::new(
        (1..n_p).into_iter().map(|i| i as f64).collect()
    ) / (n_p as f64 + 1.);

    let v = vandermonde::vandermonde(&dist_eq, n_p);

    gammas.size();
}