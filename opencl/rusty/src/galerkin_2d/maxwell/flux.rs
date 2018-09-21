use galerkin_2d::flux::{FluxScheme, FluxKey, NumericalFlux, Side};
use galerkin_2d::unknowns::{Unknown};
use galerkin_2d::grid::SpatialVariable;
use galerkin_2d::maxwell::unknowns::*;
use galerkin_2d::reference_element::ReferenceElement;

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

impl FluxKey for MaxwellFluxType {}

struct MaxwellInteriorFlux {
}

impl NumericalFlux<EH, Permittivity> for MaxwellInteriorFlux {
    fn flux(&self, minus: Side<EH, Permittivity>, plus: Side<EH, Permittivity>, outward_normal: Vec2) -> <EH as Unknown>::L {
        (minus.u + plus.u) / 2.
    }
}

pub struct Vacuum<'flux> {
    interior_flux: &'flux MaxwellInteriorFlux,
}

impl<'flux> FluxScheme<EH> for Vacuum<'flux> {
    type F = Permittivity;
    type K = MaxwellFluxType;

    fn flux_type(&self, key: MaxwellFluxType) -> & NumericalFlux<EH, Permittivity> {
        match key {
            _ => self.interior_flux
        }
    }
}
