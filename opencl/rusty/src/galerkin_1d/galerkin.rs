use galerkin_1d::unknowns::Unknown;
use galerkin_1d::grid::SpatialFlux;
use galerkin_1d::flux::FluxScheme;
use galerkin_1d::flux::NumericalFlux;

pub trait GalerkinScheme {
    type U: Unknown;
    type F: SpatialFlux;
    type FS: FluxScheme<Self::U, Self::F>;
}