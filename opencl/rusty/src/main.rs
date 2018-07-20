#[macro_use]
extern crate rulinalg;

mod functions;
mod galerkin_1d;

use galerkin_1d::advec::advec_1d_example;

fn main() {
    advec_1d_example();
}

