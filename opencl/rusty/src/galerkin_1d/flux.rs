use galerkin_1d::unknowns::Unknown;
use galerkin_1d::grid::SpatialFlux;

pub struct Side<U: Unknown, F: SpatialFlux> {
    u: U::Unit,
    f: F::Unit,
}

pub trait NumericalFlux<U: Unknown, F: SpatialFlux>: Copy {
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: f64) -> U::Unit;
}

#[derive(Clone, Copy)]
pub struct LaxFriedrichs {
    alpha: f64,
}

impl<U, F> NumericalFlux<U, F> for LaxFriedrichs where
    U: Unknown<Unit=f64>,
    F: SpatialFlux<Unit=f64>,
{
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: f64) -> U::Unit {
        let f_minus = minus.u * minus.f;
        let f_plus = plus.u * plus.f;
        let avg = (f_minus + f_plus) / 2.;
        let jump = f_minus * outward_normal - f_plus * outward_normal;
        avg + jump / 2.
    }
}

#[derive(Clone, Copy)]
pub struct FreeflowFlux {}

impl<U, F> NumericalFlux<U, F> where U: Unknown, F: SpatialFlux {
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: f64) -> U::Unit {
        U::zero()
    }
}

