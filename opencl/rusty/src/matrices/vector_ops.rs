extern crate generic_array as ga;
extern crate typenum as tn;
extern crate itertools;
extern crate rulinalg;
extern crate core;

use std::ops::{Neg, Mul, Add, Sub, Div};
use self::tn::{U1, UInt, Prod, Unsigned};
use self::ga::{GenericArray, ArrayLength};
use self::rulinalg::vector::Vector as RaVector;

pub struct Vector<N: Unsigned + ArrayLength<f64>> {
    data: ga::GenericArray<f64, N>,
}

impl<N: Unsigned + ArrayLength<f64>> Vector<N> {
    pub fn static_size() -> usize {
        <N as Unsigned>::to_usize()
    }

    pub fn size(&self) -> usize {
        Self::static_size()
    }

    pub fn from_vec(vec: Vec<f64>) -> Vector<N> {
        Vector { data: vec.into_iter().map(|x| x).collect() }
    }

    pub fn from_rulinalg(vector: &RaVector<f64>) -> Vector<N> {
        Vector { data: vector.into_iter().map(|x| *x).collect() }
    }

    pub fn from_const(x: f64) -> Vector<N> {
        Vector { data: itertools::repeat_n(x, Self::static_size()).collect() }
    }

    pub fn to_rulinalg(&self) -> RaVector<f64> {
        self.data.iter().map(|a| *a).collect()
    }
}

/**
 * Mul
 */
/* RHS=Self */
impl<N: Unsigned + ArrayLength<f64>> Mul for Vector<N> {
    type Output = Self;

    fn mul(self, rhs: Self) -> Self::Output {
        let data: GenericArray<f64, N> = self.data.into_iter().zip(rhs.data.into_iter())
            .map(|(a, b)| a * b)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Mul for &'a Vector<N> {
    type Output = Vector<N>;

    fn mul(self, rhs: Self) -> Vector<N> {
        let data: GenericArray<f64, N> = self.data.iter().zip(rhs.data.iter())
            .map(|(&a, &b)| a * b)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Mul<&'a Vector<N>> for Vector<N> {
    type Output = Self;

    fn mul(self, rhs: &Self) -> Self::Output {
        let data: GenericArray<f64, N> = self.data.iter().zip(rhs.data.iter())
            .map(|(&a, &b)| a * b)
            .collect();
        Vector { data }
    }
}

//impl<'a, 'b, N: Unsigned + ArrayLength<f64>> Mul<&'a Vector<N>> for &'b Vector<N> {
//    type Output = Vector<N>;
//
//    fn mul(self, rhs: Self) -> Vector<N> {
//        let data: GenericArray<f64, N> = self.data.into_iter().zip(rhs.data.into_iter())
//            .map(|(a, b)| a * b)
//            .collect();
//        Vector { data }
//    }
//}

/* RHS=f64 */
impl<N: Unsigned + ArrayLength<f64>> Mul<f64> for Vector<N> {
    type Output = Self;

    fn mul(self, rhs: f64) -> Self::Output {
        let data: GenericArray<f64, N> = self.data.into_iter()
            .map(|a| a * rhs)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Mul<f64> for &'a Vector<N> {
    type Output = Vector<N>;

    fn mul(self, rhs: f64) -> Vector<N> {
        let data: GenericArray<f64, N> = self.data.iter()
            .map(|&a| a * rhs)
            .collect();
        Vector { data }
    }
}
/**
 * Add
 */
/* RHS=Self */
impl<N: Unsigned + ArrayLength<f64>> Add for Vector<N> {
    type Output = Self;

    fn add(self, rhs: Self) -> Self::Output {
        let data: GenericArray<f64, N> = self.data.into_iter().zip(rhs.data.into_iter())
            .map(|(a, b)| a + b)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Add<&'a Self> for Vector<N> {
    type Output = Self;

    fn add(self, rhs: &Self) -> Self::Output {
        let data: GenericArray<f64, N> = self.data.into_iter().zip(rhs.data.iter())
            .map(|(a, &b)| a + b)
            .collect();
        Vector { data }
    }
}

impl<N: Unsigned + ArrayLength<f64>> Add<f64> for Vector<N> {
    type Output = Self;

    fn add(self, rhs: f64) -> Self {
        let data: GenericArray<f64, N> = self.data.into_iter()
            .map(|a| a + rhs)
            .collect();
        Vector { data }
    }
}

/**
 * Div
 */
impl<N: Unsigned + ArrayLength<f64>> Div for Vector<N> {
    type Output = Self;

    fn div(self, rhs: Self) -> Self {
        let data: GenericArray<f64, N> = self.data.into_iter().zip(rhs.data.into_iter())
            .map(|(a, b)| a / b)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Div<&'a Self> for Vector<N> {
    type Output = Self;

    fn div(self, rhs: &Self) -> Self {
        let data: GenericArray<f64, N> = self.data.into_iter().zip(rhs.data.iter())
            .map(|(a, &b)| a / b)
            .collect();
        Vector { data }
    }
}

impl<N: Unsigned + ArrayLength<f64>> Div<f64> for Vector<N> {
    type Output = Self;

    fn div(self, rhs: f64) -> Self {
        let data: GenericArray<f64, N> = self.data.into_iter()
            .map(|a| a / rhs)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Div<f64> for &'a Vector<N> {
    type Output = Vector<N>;

    fn div(self, rhs: f64) -> Vector<N> {
        let data: GenericArray<f64, N> = self.data.iter()
            .map(|&a| a / rhs)
            .collect();
        Vector { data }
    }
}

/**
 * Sub
 */
impl<N: Unsigned + ArrayLength<f64>> Sub for Vector<N> {
    type Output = Self;

    fn sub(self, rhs: Self) -> Self {
        let data: GenericArray<f64, N> = self.data.into_iter().zip(rhs.data.into_iter())
            .map(|(a, b)| a - b)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Sub for &'a Vector<N> {
    type Output = Vector<N>;

    fn sub(self, rhs: Self) -> Vector<N> {
        let data: GenericArray<f64, N> = self.data.iter().zip(rhs.data.iter())
            .map(|(&a, &b)| a - b)
            .collect();
        Vector { data }
    }
}

impl<N: Unsigned + ArrayLength<f64>> Sub<f64> for Vector<N> {
    type Output = Self;

    fn sub(self, rhs: f64) -> Self {
        let data: GenericArray<f64, N> = self.data.into_iter()
            .map(|a| a - rhs)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Sub<f64> for &'a Vector<N> {
    type Output = Vector<N>;

    fn sub(self, rhs: f64) -> Vector<N> {
        let data: GenericArray<f64, N> = self.data.iter()
            .map(|&a| a - rhs)
            .collect();
        Vector { data }
    }
}

/**
 * Neg
 */
impl<N: Unsigned + ArrayLength<f64>> Neg for Vector<N> {
    type Output = Self;

    fn neg(self) -> Self::Output {
        let data: GenericArray<f64, N> = self.data.into_iter()
            .map(|a| -a)
            .collect();
        Vector { data }
    }
}

impl<'a, N: Unsigned + ArrayLength<f64>> Neg for &'a Vector<N> {
    type Output = Vector<N>;

    fn neg(self) -> Vector<N> {
        let data: GenericArray<f64, N> = self.data.iter()
            .map(|&a| -a)
            .collect();
        Vector { data }
    }
}
