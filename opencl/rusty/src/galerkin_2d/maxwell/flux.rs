use galerkin_2d::flux::{FluxKey, FluxScheme, NumericalFlux, Side};
use galerkin_2d::grid::SpatialVariable;
use galerkin_2d::grid::Vec2;
use galerkin_2d::maxwell::unknowns::*;
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::unknowns::Unknown;
use rulinalg::vector::Vector;

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

impl SpatialVariable for () {
    type Line = ();

    fn edge_1(&self, reference_element: &ReferenceElement) -> () {
        ()
    }

    fn edge_2(&self, reference_element: &ReferenceElement) -> () {
        ()
    }

    fn edge_3(&self, reference_element: &ReferenceElement) -> () {
        ()
    }

    fn face1_zero(reference_element: &ReferenceElement) -> () {
        ()
    }

    fn face2_zero(reference_element: &ReferenceElement) -> () {
        ()
    }

    fn face3_zero(reference_element: &ReferenceElement) -> () {
        ()
    }
}

#[derive(Debug, Copy, Clone)]
pub enum MaxwellFluxType {
    Interior,
    Exterior,
}

impl FluxKey for MaxwellFluxType {}

#[derive(Debug)]
pub struct Vacuum {}

impl Vacuum {
    fn interior_flux(
        minus: Side<EH, ()>,
        plus: Side<EH, ()>,
        outward_normal: &Vec<Vec2>,
    ) -> EH {
        let d_eh = minus.u - plus.u;
        let (d_hx, d_hy, d_ez) = (d_eh.Hx, d_eh.Hy, d_eh.Ez);
        Self::flux_calculation(&d_hx, &d_hy, &d_ez, outward_normal)
    }

    fn exterior_flux(
        minus: Side<EH, ()>,
        plus: Side<EH, ()>,
        outward_normal: &Vec<Vec2>,
    ) -> EH {
        let d_hx = Vector::zeros(minus.u.Hx.size());
        let d_hy = Vector::zeros(minus.u.Hy.size());
        let d_ez = &minus.u.Ez * 2.;
        Self::flux_calculation(&d_hx, &d_hy, &d_ez, outward_normal)
    }

    fn flux_calculation(
        d_hx: &Vector<f64>,
        d_hy: &Vector<f64>,
        d_ez: &Vector<f64>,
        outward_normal: &Vec<Vec2>,
    ) -> EH {
        let alpha = 1.;
        let (n_x, n_y): (Vector<f64>, Vector<f64>) = (
            outward_normal.iter().map(|ref n| n.x).collect(),
            outward_normal.iter().map(|ref n| n.y).collect(),
        );

        let n_dot_dh = &d_hx.elemul(&n_x) + &d_hy.elemul(&n_y);
        let flux_hx = &d_ez.elemul(&n_y) + (&n_dot_dh.elemul(&n_x) - d_hx) * alpha;
        let flux_hy = -&d_ez.elemul(&n_x) + (&n_dot_dh.elemul(&n_y) - d_hy) * alpha;
        let flux_ez = -&d_hy.elemul(&n_x) + &d_hx.elemul(&n_y) - d_ez * alpha;

        EH {
            Ez: flux_ez,
            Hx: flux_hx,
            Hy: flux_hy,
        }
    }
}

impl FluxScheme<EH> for Vacuum {
    // In the vacuum, we can normalize all constants to 1, and there is no spatial variation
    // of the permittivity, so the spatial variable is ().
    type F = ();
    type K = MaxwellFluxType;

    fn flux_type(
        key: Self::K,
        minus: Side<EH, ()>,
        plus: Side<EH, ()>,
        outward_normal: &Vec<Vec2>,
    ) -> EH {
        match key {
            MaxwellFluxType::Interior => Vacuum::interior_flux(minus, plus, &outward_normal),
            MaxwellFluxType::Exterior => Vacuum::exterior_flux(minus, plus, &outward_normal),
        }
    }
}
