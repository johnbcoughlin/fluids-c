#[macro_use]
extern crate rulinalg;
extern crate gnuplot;

mod functions;
mod galerkin_1d;
//mod galerkin_2d;
mod plotter;
mod distmesh;

//use galerkin_1d::advec::advec_1d_example;
use galerkin_1d::maxwell::maxwell_1d_example;

fn main() {
    maxwell_1d_example();
}

