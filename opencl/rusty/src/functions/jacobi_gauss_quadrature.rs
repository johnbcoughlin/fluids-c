extern crate rulinalg;

use functions::gamma::gamma;

pub fn jacobi_gauss_quadrature(alpha: f64, beta: f64, n: i32) -> JacobiGaussQuadrature {
        return JacobiGaussQuadrature {
            xs: vec![-(alpha - beta) / (alpha + beta + 2.0)],
            ws: vec![2.0],
        };
}

pub struct JacobiGaussQuadrature {
    xs: Vec<f64>,
    ws: Vec<f64>,
}