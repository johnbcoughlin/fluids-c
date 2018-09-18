use galerkin_2d::grid::SpatialVariable;
use galerkin_2d::grid::Vec2;
use galerkin_2d::unknowns::Unknown;

pub struct Side<U, F>
where
    U: Unknown,
    F: SpatialVariable,
{
    pub u: U::Line,
    pub f: F::Line,
}

pub trait FluxKey {}

pub trait FluxScheme<U, F, K>
where
    U: Unknown,
    F: SpatialFlux,
    K: FluxKey,
{
    fn flux_type<'flux>(key: K) -> &'flux dyn NumericalFlux<U, F>;
}

pub trait NumericalFlux<U, F>: Copy {
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: Vec2) -> U::Line;
}
