extern crate rulinalg;

use galerkin_2d::unknowns::{Unknown};


pub trait GalerkinScheme {
    type U: Unknown;
//    type F: SpatialFlux;
//    type FS: FluxScheme<Self::U, Self::F>;


}