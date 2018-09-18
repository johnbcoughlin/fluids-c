use galerkin_1d::grid::SpatialFlux;
use galerkin_1d::unknowns::Unknown;

pub struct Side<U: Unknown, F: SpatialFlux> {
    pub u: U::Unit,
    pub f: F::Unit,
}

pub trait NumericalFlux<U: Unknown, F: SpatialFlux>: Copy {
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: f64) -> U::Unit;
}

pub trait FluxScheme<U, F>
where
    U: Unknown,
    F: SpatialFlux,
{
    type Left: NumericalFlux<U, F>;
    type Right: NumericalFlux<U, F>;
    type Interior: NumericalFlux<U, F>;
}

pub enum FluxEnum<U: Unknown, F: SpatialFlux, FS: FluxScheme<U, F>> {
    Left(FS::Left),
    Right(FS::Right),
    Interior(FS::Interior),
}

#[derive(Clone, Copy)]
pub struct LaxFriedrichs {
    pub alpha: f64,
}

impl<U, F> NumericalFlux<U, F> for LaxFriedrichs
where
    U: Unknown<Unit = f64>,
    F: SpatialFlux<Unit = f64>,
{
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: f64) -> U::Unit {
        let f_minus = minus.u * minus.f;
        let f_plus = plus.u * plus.f;
        let avg = (f_minus + f_plus) / 2.;
        let jump = f_minus * outward_normal - f_plus * outward_normal;

        let f_numerical = avg + jump / 2.;

        (minus.u * minus.f - f_numerical) * outward_normal
    }
}

#[derive(Clone, Copy)]
pub struct FreeflowFlux {}

impl<U, F> NumericalFlux<U, F> for FreeflowFlux
where
    U: Unknown,
    F: SpatialFlux,
{
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: f64) -> U::Unit {
        U::zero()
    }
}
