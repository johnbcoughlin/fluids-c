extern crate num;

use num::complex::Complex64;
use std::convert;
mod functions;
use functions::gamma::gamma;

fn main() {
//    let n: i32 = 8;
//    let mesh: Mesh = generate_mesh_1d(0.0, 2.0, 10);
//
//    let z: Complex64 = Complex64::from(5 as f64);
//    println!("gamma(5) = {}", gamma(z));
}

fn generate_mesh_1d(x_min: f64, x_max: f64, k: i32) -> Mesh {
    let n_nodes = k + 1;
    let xs: Vec<f64> = (0..n_nodes).map(|i| {
        (x_max - x_min) * ((i + 1) as f64) / (k as f64) + x_min
    }).collect();
    let element_to_nodes: Vec<(i32, i32)> = (0..n_nodes).map(|i| {
        (i, i+1)
    }).collect();
    return Mesh {
        k,
        n_nodes,
        xs,
        element_to_nodes,
    }
}

struct Mesh {
    // the number of elements
    k: i32,
    // the number of nodes
    n_nodes: i32,
    // the x-coordinates of nodes
    xs: Vec<f64>,
    // associative array from element indices to the node indices which they connect to:
    element_to_nodes: Vec<(i32, i32)>,
}