extern crate rulinalg;

use std::iter::repeat;
use distmesh::distmesh_2d::ellipse;
use galerkin_2d::galerkin::GalerkinScheme;
use galerkin_2d::grid::{assemble_grid, Grid, SpatialVariable};
use galerkin_2d::maxwell::flux::*;
use galerkin_2d::maxwell::unknowns::*;
use galerkin_2d::operators::{assemble_operators, Operators};
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::unknowns::{communicate, initialize_storage, Unknown};
use rulinalg::vector::Vector;
use galerkin_2d::grid::Element;
use galerkin_2d::flux::compute_flux;
use galerkin_2d::operators::grad;
use galerkin_2d::operators::curl_2d;

pub struct Maxwell2D {
    flux_scheme: Vacuum,
}

impl GalerkinScheme for Maxwell2D {
    type U = EH;
    type FS = Vacuum;
}

type EHElement<'grid> = Element<'grid, Maxwell2D>;

pub fn maxwell_2d<'grid, Fx>(
    grid: &Grid<Maxwell2D>,
    reference_element: &ReferenceElement,
    operators: &Operators,
    u_0: Fx,
) where
    Fx: Fn(&Vector<f64>) -> EH,
{
    let final_time = 1.0;
    let dt = 0.003668181816046;

    let mut t: f64 = 0.0;

    let mut storage: Vec<EHStorage> = initialize_storage(
        u_0,
        reference_element.n_p as i32,
        reference_element,
        grid,
        operators,
    );

    let mut residuals: Vec<EH> = repeat(EH::zero(reference_element))
        .take(grid.elements.len()).collect();

    while t < final_time {
        for int_rk in 0..5 {
            communicate(t, reference_element, grid, &storage);

            for elt in (*grid).elements.iter() {
                let mut storage = &mut storage[elt.index as usize];

                let residuals_eh = {
                    let residuals_eh = &(residuals[elt.index as usize]);
                    let rhs = maxwell_rhs_2d(&elt, &storage, &operators);
                };
            }
        }
    }
}

fn maxwell_rhs_2d<'grid>(elt: &EHElement<'grid>, elt_storage: &EHStorage, operators: &Operators) -> EH {
    let (face1_flux, face2_flux, face3_flux) = compute_flux(elt, elt_storage);
    let flux = EH::lift_faces(&operators.lift, &face1_flux, &face2_flux, &face3_flux);

    let grad_ez = grad(&elt_storage.u_k.Ez, operators, &elt.r_x, &elt.s_x, &elt.r_y, &elt.s_y);
    let curl_h = curl_2d(&elt_storage.u_k.Hx, &elt_storage.u_k.Hy, operators,
                         &elt.r_x, &elt.s_x, &elt.r_y, &elt.s_y);

    let Hx = -grad_ez.y +
}

pub fn maxwell_2d_example() {
    let n_p = 10;
    let reference_element = ReferenceElement::legendre(n_p);
    let operators = assemble_operators(&reference_element);
    let mesh = ellipse();
    let boundary_condition = |t| EH::face1_zero(&reference_element);
    let grid: Grid<Maxwell2D> =
        assemble_grid(&reference_element, &operators, &mesh, &boundary_condition);
}

#[cfg(test)]
mod tests {
    use super::maxwell_2d_example;

    #[test]
    pub fn test_maxwell_2d() {
        maxwell_2d_example();
    }
}
