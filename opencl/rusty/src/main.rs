#![feature(associated_type_defaults)]

#[macro_use]
extern crate rulinalg;
extern crate gnuplot;

mod distmesh;
mod functions;
mod galerkin_1d;
mod galerkin_2d;
mod plot;

//use galerkin_1d::advec::advec_1d_example;
//use galerkin_1d::maxwell::maxwell_1d_example;
use galerkin_2d::maxwell::maxwell::maxwell_2d_example;

fn main() {
    maxwell_2d_example();
}
