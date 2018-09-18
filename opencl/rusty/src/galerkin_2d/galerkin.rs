extern crate rulinalg;

use galerkin_2d::flux::FluxScheme;
use galerkin_2d::grid::SpatialVariable;
use galerkin_2d::unknowns::Unknown;
use galerkin_2d::flux::FluxKey;

pub trait GalerkinScheme {
    type U: Unknown;
    type F: SpatialVariable;
    type FK: FluxKey;
    type FS: FluxScheme<Self::U, Self::F, Self::FK>;
}
