use galerkin_2d::grid::SpatialVariable;
use galerkin_2d::grid::Vec2;
use galerkin_2d::unknowns::Unknown;

pub struct Side<U, F>
where
    U: Unknown,
    F: SpatialVariable,
{
    pub u: <U as SpatialVariable>::Line,
    pub f: F::Line,
}

pub trait FluxKey {}

pub trait FluxScheme<U>
where
    U: Unknown,
{
    type F: SpatialVariable;

    type K: FluxKey;

    fn flux_type(&self, key: Self::K) -> & dyn NumericalFlux<U, Self::F>;
}

pub trait NumericalFlux<U, F>
where
    U: Unknown,
    F: SpatialVariable,
{
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: Vec2) -> U::L;
}
