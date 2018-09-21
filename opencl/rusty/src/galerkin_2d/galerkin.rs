extern crate rulinalg;

use galerkin_2d::flux::FluxScheme;
use galerkin_2d::unknowns::Unknown;

pub trait GalerkinScheme {
    type U: Unknown;
    type FS: FluxScheme<Self::U>;
}
