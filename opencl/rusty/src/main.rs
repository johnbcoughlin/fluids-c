#[macro_use]
extern crate rulinalg;
extern crate gnuplot;

mod functions;
mod galerkin_1d;

use galerkin_1d::advec::advec_1d_example;
use self::gnuplot::{Figure, Caption, Color};

fn main() {
    let (xs, us) = advec_1d_example();

    let mut fig = Figure::new();
//    fig.set_terminal("qt", "");
    fig.axes2d()
        .lines(xs.as_slice(), us.as_slice(), &[Caption("A line"), Color("black")]);
    fig.show();
}

