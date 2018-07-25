extern crate rulinalg;

use functions::range_kutta::{RKA, RKB, RKC};
use galerkin_1d::unknowns::{ElementStorage, Unknown, communicate, initialize_storage};
use rulinalg::vector::Vector;
use galerkin_1d::grid;
use galerkin_1d::operators::{Operators, assemble_operators};
use std::iter::repeat;

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

    fn zero() -> EHUnit {
        EHUnit { E: 0.0, H: 0.0 }
    }
}

struct Permittivity {
    epsilon: Vector<f64>,
    mu: Vector<f64>,
}

impl grid::SpatialFlux for Permittivity {}

type Grid = grid::Grid<EH, Permittivity>;

type Element = grid::Element<EH, Permittivity>;

type EHStorage = ElementStorage<EH>;

fn permittivity(xs: &Vector<f64>) -> Permittivity {
    Permittivity {
        epsilon: xs.iter().map(|x: &f64| if *x > 0.0 { 2.0 } else { 1.0 }).collect(),
        mu: xs.iter().map(|x: &f64| 1.0).collect(),
    }
}

pub fn maxwell_1d_example() {
    let n_p = 6;
    let reference_element = grid::ReferenceElement::legendre(n_p);
    let left_boundary_face = grid::Face::Boundary(Box::new(move |_: f64, other_side: EHUnit|
        EHUnit { E: 0.0, H: other_side.H }
    ));
    let right_boundary_face = grid::Face::Boundary(Box::new(move |_: f64, other_side: EHUnit|
        EHUnit { E: 0.0, H: other_side.H }
    ));
    let grid: grid::Grid<EH, Permittivity> = grid::generate_grid(
        -1.0, 1.0, 80, &reference_element, left_boundary_face,
        right_boundary_face, &permittivity);
    let operators = assemble_operators::<EH>(&reference_element);
}

fn maxwell_1d<Fx>(eh_0: Fx, grid: &Grid, reference_element: &grid::ReferenceElement,
                  operators: &Operators)
    where Fx: Fn(&Vector<f64>) -> EH {
    let final_time = 4.5;
    let cfl = 0.75;
    let x_scale = 0.01;
    let dt: f64 = 0.5 * cfl / (consts::PI * 2.) * x_scale;
    let n_t = (final_time / dt).ceil() as i32;
    let dt = final_time / n_t as f64;

    let mut t: f64 = 0.0;

    let mut storage: Vec<EHStorage> = initialize_storage(eh_0, reference_element.n_p,
                                                         grid, operators);
    let mut residuals: Vec<(Vector<f64>, Vector<f64>)> =
        repeat((
            Vector::zeros(reference_element.n_p as usize + 1),
            Vector::zeros(reference_element.n_p as usize + 1),
        ))
            .take(grid.elements.len())
            .collect();

    for _ in 0..n_t {
        for int_rk in 0..5 {
            let t = t + RKC[int_rk] * dt;

            communicate(t, grid, &storage);

            for elt in (*grid).elements.iter() {
                let mut storage = &mut storage[elt.index as usize];

                let (residuals_e, residuals_h) = {
                    let residuals_eh = &(residuals[elt.index as usize]);
                    let (residuals_e, residuals_h) = residuals_eh;
                    let (rhs_e, rhs_h) = maxwell_rhs_1d(&elt, &storage, &operators);
                }
            }
        }
    }
}

fn maxwell_rhs_1d(elt: &Element, elt_storage: &EHStorage,
                  operators: &Operators) -> (Vector<f64>, Vector<f64>) {
    let de_left = {
        let e
    }
}
