extern crate rulinalg;

use std::fmt;
use std::cell::{Cell, RefCell};
use self::rulinalg::vector::Vector;
use galerkin_1d::grid;
use galerkin_1d::operators::Operators;

pub trait Unknown {
    type Unit: Copy + fmt::Debug;

    fn first(&self) -> Self::Unit;

    fn last(&self) -> Self::Unit;
}

pub struct ElementStorage<U: Unknown> {

    // The derivative of r with respect to x, i.e. the metric of the x -> r mapping.
    pub r_x: Vector<f64>,
    pub r_x_at_faces: Vector<f64>,

    pub u_k: U,
    // the interior value on the left face
    pub u_left_minus: Cell<Option<U::Unit>>,
    // the exterior value on the left face
    pub u_left_plus: Cell<Option<U::Unit>>,
    // the interior value on the right face
    pub u_right_minus: Cell<Option<U::Unit>>,
    // the exterior value on the right face
    pub u_right_plus: Cell<Option<U::Unit>>,
}

impl<U: Unknown> fmt::Debug for ElementStorage<U> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{{\n")?;
        write!(f, "\tu_left_minus: {:?},\n", self.u_left_minus)?;
        write!(f, "\tu_left_plus: {:?},\n", self.u_left_plus)?;
        write!(f, "\tu_right_minus: {:?},\n", self.u_right_minus)?;
        write!(f, "\tu_right_plus: {:?},\n", self.u_right_minus)?;
        write!(f, "}}")
    }
}

pub fn initialize_storage<U, Fx>(u_0: Fx, n_p: i32, grid: &grid::Grid<U>, operators: &Operators)
                             -> Vec<ElementStorage<U>>
    where U: Unknown,
          Fx: Fn(&Vector<f64>) -> U {
    grid.elements.iter().map(|elt| {
        let d_r_x_k = &operators.d_r * &elt.x_k;
        let r_x = Vector::ones(d_r_x_k.size()).elediv(&d_r_x_k);
        let r_x_at_faces = vector![r_x[0], r_x[n_p as usize]];
        ElementStorage {
            r_x,
            r_x_at_faces,
            u_k: u_0(&elt.x_k),
            u_left_minus: Cell::new(None),
            u_right_minus: Cell::new(None),
            u_left_plus: Cell::new(None),
            u_right_plus: Cell::new(None),
        }
    }).collect()
}

// Pass flux information across faces into each element's local storage.
pub fn communicate<U>(t: f64, grid: &grid::Grid<U>, storages: &Vec<ElementStorage<U>>)
    where U: Unknown {
    for (i, elt) in grid.elements.iter().enumerate() {
        let mut storage = storages.get(i).expect("index mismatch");
        let mut u_k: &U = &storage.u_k;
        let (minus, plus) = match *elt.left_face {
            grid::Face::Neumann(_) => (None, None),
            grid::Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (
                    // minus is outside, plus is inside
                    Some(u_0),
                    Some(u_k.first())
                )
            }
            grid::Face::Interior(j) => {
                let u_k_minus_1: &U = &storages[j as usize].u_k;
                (
                    // minus is outside, plus is inside
                    Some(u_k_minus_1.last()),
                    Some(u_k.first())
                )
            }
        };
        storage.u_left_minus.set(minus);
        storage.u_left_plus.set(plus);

        let (minus, plus) = match *elt.right_face {
            grid::Face::Neumann(_) => (None, None),
            grid::Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (
                    // minus is outside, plus is inside
                    Some(u_0),
                    Some(u_k.first())
                )
            }
            grid::Face::Interior(j) => {
                let u_k_plus_1: &U = &storages[j as usize].u_k;
                (
                    // minus is outside, plus is inside
                    Some(u_k_plus_1.first()),
                    Some(u_k.last())
                )
            }
        };
        storage.u_right_minus.set(minus);
        storage.u_right_plus.set(plus);
    }
}

