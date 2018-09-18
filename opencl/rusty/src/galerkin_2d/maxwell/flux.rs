use galerkin_2d::flux::FluxKey;
use galerkin_2d::flux::{FluxScheme, NumericalFlux};
use galerkin_2d::grid::SpatialVariable;
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::unknowns::Unknown;
use galerkin_2d::maxwell::unknowns::*;

#[derive(Copy, Clone)]
pub struct Permittivity {
    epsilon: f64,
    mu: f64,
}

impl Permittivity {
    fn zero() -> Self {
        Permittivity {
            epsilon: 0.,
            mu: 0.,
        }
    }
}

impl SpatialVariable for Permittivity {
    type Line = Permittivity;

    fn edge_1(&self, reference_element: &ReferenceElement) -> Permittivity {
        self.clone()
    }

    fn edge_2(&self, reference_element: &ReferenceElement) -> Permittivity {
        self.clone()
    }

    fn edge_3(&self, reference_element: &ReferenceElement) -> Permittivity {
        self.clone()
    }

    fn face1_zero(reference_element: &ReferenceElement) -> Permittivity {
        Permittivity::zero()
    }

    fn face2_zero(reference_element: &ReferenceElement) -> Permittivity {
        Permittivity::zero()
    }

    fn face3_zero(reference_element: &ReferenceElement) -> Permittivity {
        Permittivity::zero()
    }
}

pub enum MaxwellFluxType {
    Interior,
}

pub struct Vacuum {}

impl FluxScheme<EH, Permittivity, MaxwellFluxType> for Vacuum {
    fn flux_type<'flux>(key: MaxwellFluxType) -> &'flux NumericalFlux<EH, Permittivity> {
        match key {
            _ => Permittivity {
                epsilon: 1.,
                mu: 1.,
            }
        }
    }
}
