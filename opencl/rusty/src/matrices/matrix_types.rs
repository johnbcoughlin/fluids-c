extern crate generic_array as ga;
extern crate typenum as tn;
extern crate blas;
extern crate rulinalg;
extern crate generic_array;
extern crate itertools;
extern crate core;

use self::tn::{U1, UInt, Prod, Integer};
use std::ops::{Neg, Mul, Add, Sub};
use self::ga::{GenericArray, ArrayLength};
use self::rulinalg::vector::Vector as RaVector;
use self::rulinalg::matrix::Matrix as RaMatrix;

pub trait Dim where
    Self: ArrayLength<f64>,
{}

pub struct Matrix<N, M> where
    N: Dim + Add<M>,
    <N as Add<M>>::Output: ArrayLength<f64>,
{
    data: ga::GenericArray<f64, <N as Add<M>>::Output>,
}

