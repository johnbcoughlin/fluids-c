extern crate rulinalg;

use galerkin_1d::unknowns::{Unknown, communicate, initialize_storage};
use rulinalg::vector::Vector;
use galerkin_1d::grid;

struct EH {
    E: Vector<f64>,
    H: Vector<f64>,
}

#[derive(Debug, Copy, Clone)]
struct EHUnit {
    E: f64,
    H: f64,
}

impl Unknown for EH {
    type Unit = EHUnit;

    fn first(&self) -> Self::Unit {
        EHUnit { E: self.E[0], H: self.H[0] }
    }

    fn last(&self) -> Self::Unit {
        EHUnit { E: self.E[self.E.size() - 1], H: self.H[self.H.size() - 1] }
    }
}

struct Permittivity {
    epsilon: Vector<f64>,
    mu: Vector<f64>,
}

impl grid::SpatialFlux for Permittivity {
}

