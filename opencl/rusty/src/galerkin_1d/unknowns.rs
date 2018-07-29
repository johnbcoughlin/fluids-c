extern crate rulinalg;

use std::fmt;
use std::cell::{Cell, RefCell};
use self::rulinalg::vector::Vector;
use galerkin_1d::grid::ElementStorage;
use galerkin_1d::grid;
use galerkin_1d::operators::Operators;

pub trait Unknown {
    type Unit: Copy + fmt::Debug;

    fn first(&self) -> Self::Unit;

    fn last(&self) -> Self::Unit;

    fn zero() -> Self::Unit;
}

pub fn initialize_storage<U, F, Fx>(u_0: Fx, n_p: i32, grid: &grid::Grid<U, F>, operators: &Operators)
                                    -> Vec<ElementStorage<U, F>>
    where U: Unknown,
          F: grid::SpatialFlux,
          Fx: Fn(&Vector<f64>) -> U {
    let mut result: Vec<ElementStorage<U, F>> = vec![];
    for (i, elt) in grid.elements.iter().enumerate() {
        let d_r_x_k = &operators.d_r * &elt.x_k;
        let r_x = Vector::ones(d_r_x_k.size()).elediv(&d_r_x_k);
        let r_x_at_faces = vector![r_x[0], r_x[n_p as usize]];

        // minus is interior, plus is exterior
        let (f_left_minus, f_left_plus) = match *elt.left_face {
            grid::Face::Interior(j) => (
                elt.spatial_flux.first(),
                grid.elements[j as usize].spatial_flux.last(),
            ),
            grid::Face::Boundary(_, f) => (
                elt.spatial_flux.first(),
                f,
            )
        };

        let (f_right_minus, f_right_plus) = match *elt.right_face {
            grid::Face::Interior(j) => (
                elt.spatial_flux.last(),
                grid.elements[j as usize].spatial_flux.first(),
            ),
            grid::Face::Boundary(_, f) => (
                elt.spatial_flux.last(),
                f,
            ),
        };

        result.push(ElementStorage {
            r_x,
            r_x_at_faces,
            u_k: u_0(&elt.x_k),
            u_left_minus: Cell::new(U::zero()),
            u_right_minus: Cell::new(U::zero()),
            u_left_plus: Cell::new(U::zero()),
            u_right_plus: Cell::new(U::zero()),
            f_left_minus: Cell::new(f_left_minus),
            f_left_plus: Cell::new(f_left_plus),
            f_right_minus: Cell::new(f_right_minus),
            f_right_plus: Cell::new(f_right_plus),
        });
    }
    result
}

// Pass flux information across faces into each element's local storage.
pub fn communicate<U, F>(t: f64, grid: &grid::Grid<U, F>, storages: &Vec<ElementStorage<U, F>>)
    where U: Unknown,
          F: grid::SpatialFlux {
    for (i, elt) in grid.elements.iter().enumerate() {
        let mut storage = storages.get(i).expect("index mismatch");
        let mut u_k: &U = &storage.u_k;
        let first = u_k.first();
        let (u_left_minus, u_left_plus) = match *elt.left_face {
            grid::Face::Interior(j) => {
                let u_k_minus_1: &U = &storages[j as usize].u_k;
                // minus is interior, plus is exterior
                (
                    first,
                    u_k_minus_1.last(),
                )
            }
            grid::Face::Boundary(ref b, _) => {
                let bc = b(t, first);
                (
                    first,
                    bc,
                )
            }
        };
        storage.u_left_minus.set(u_left_minus);
        storage.u_left_plus.set(u_left_plus);

        let last = u_k.last();
        let (u_right_minus, u_right_plus) = match *elt.right_face {
            grid::Face::Interior(j) => {
                let u_k_plus_1: &U = &storages[j as usize].u_k;
                // minus is interior, plus is exterior
                (
                    last,
                    u_k_plus_1.first(),
                )
            }
            grid::Face::Boundary(ref b, _) => {
                let bc = b(t, first);
                (
                    last,
                    bc,
                )
            }
        };
        storage.u_right_minus.set(u_right_minus);
        storage.u_right_plus.set(u_right_plus);
    }
}

