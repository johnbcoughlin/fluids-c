extern crate rulinalg;
extern crate core;

use std::fmt;
use std::ops::{Neg, Add, Mul, Div};

pub trait Unknown {
    type Unit: Neg<Output=Self::Unit> +
    Add<Output=Self::Unit> +
    Mul<f64, Output=Self::Unit> +
    Div<f64, Output=Self::Unit> +
    Copy + fmt::Debug;

    type Line: Neg<Output=Self::Line> +
    Add<Output=Self::Line> +
    Mul<f64, Output=Self::Line> +
    Div<f64, Output=Self::Line> +
    Copy + fmt::Debug;

    fn edge_1(&self) -> Self::Line;

    fn edge_2(&self) -> Self::Line;

    fn edge_3(&self) -> Self::Line;

    fn zero() -> Self::Unit;
}
