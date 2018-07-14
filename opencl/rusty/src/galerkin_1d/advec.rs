extern crate arrayfire;

use galerkin_1d::grid::{Element, ReferenceElement, Grid, Face};
use std::num;
use std::cell::Cell;
use self::arrayfire::{Array, Dim4, matmul, mul, div, Convertable, MatProp};
use galerkin_1d::operators::{Operators, assemble_operators};

pub fn advec_1d<Fx>(u_0: Fx, grid: &Grid, reference_element: &ReferenceElement,
                    operators: &Operators)
    where Fx: Fn(&Array) -> Array {
    let nt = 100;

    let mut storage: Vec<ElementStorage> = initialize_storage(u_0, grid, operators);
    for t in 1..nt {
        for int_rk in 1..5 {
            communicate(t as f32, grid, reference_element.n_p, &storage);
            for elt in (*grid).elements.iter() {}
        }
    }
}

fn advec_rhs_1d(elt: &Element, elt_storage: &ElementStorage, operators: &Operators) {
    let du_left = match *elt.left_face {
        Face::Neumann(f) => f,
        _ => lax_friedrichs(
            operators,
            elt_storage.u_left_minus.get().expect("Non-Neumann face should have populated u_minus"),
            elt_storage.u_right_plus.get().expect("Non-Neumann face should have populated u_plus"),
            elt.left_outward_normal,
        ),
    } as f32;
    let du_right = match *elt.right_face {
        Face::Neumann(f) => f,
        _ => lax_friedrichs(
            operators,
            elt_storage.u_right_minus.get().expect("Non-Neumann face should have populated u_minus"),
            elt_storage.u_right_plus.get().expect("Non-Neumann face should have populated u_plus"),
            elt.right_outward_normal,
        ),
    } as f32;
    let du: Array = Array::new(&[du_left, du_right], Dim4::new(&[2, 1, 1, 1]));
}

fn lax_friedrichs(operators: &Operators, u_minus: f32, u_plus: f32, outward_normal: f32) -> f32 {
    let f_a = u_minus * operators.a;
    let f_b = u_plus * operators.a;
    ;
    let avg = (f_a + f_b) / 2.;
    let jump = (u_minus * (-outward_normal)) + (u_plus * outward_normal);
    avg + operators.a.abs() * jump / 2.
}

fn initialize_storage<Fx>(u_0: Fx, grid: &Grid, operators: &Operators) -> Vec<ElementStorage>
    where Fx: Fn(&Array) -> Array {
    grid.elements.iter().map(|elt| {
        ElementStorage {
            r_x: div(&(1.0 as f32), &matmul(&operators.d_r, &elt.x_k, MatProp::NONE, MatProp::NONE), false),
            u_k: u_0(&elt.x_k),
            u_left_minus: Cell::new(None),
            u_right_minus: Cell::new(None),
            u_left_plus: Cell::new(None),
            u_right_plus: Cell::new(None),
        }
    }).collect()
}

fn array_to_vector(size: usize, array: &Array) -> Vec<f32> {
    let mut result: Vec<f32> = vec![0.0; size];
    array.eval();
    array.host(result.as_mut_slice());
    result
}

// Pass flux information across faces into each element's local storage.
fn communicate(t: f32, grid: &Grid, n_p: i32, storages: &Vec<ElementStorage>) {
    let size: usize = n_p as usize + 1;
    for (i, elt) in grid.elements.iter().enumerate() {
        let storage = storages.get(i).expect("index mismatch");
        let mut u_k = array_to_vector(size, &storage.u_k);
        let (minus, plus) = match *elt.left_face {
            Face::Neumann(_) => (None, None),
            Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (Some(u_0), Some(u_0))
            }
            Face::Interior(j) => {
                let u_k_minus_1 = array_to_vector(size, &storages[j as usize].u_k);
                (
                    Some(*u_k_minus_1.last().expect("vector u_k must not be empty")),
                    Some(*u_k.first().expect("vector u_k must not be empty"))
                )
            }
        };
        storage.u_left_minus.set(minus);
        storage.u_left_plus.set(plus);

        let (minus, plus) = match *elt.right_face {
            Face::Neumann(_) => (None, None),
            Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (Some(u_0), Some(u_0))
            }
            Face::Interior(j) => {
                let u_k_plus_1 = array_to_vector(size, &storages[j as usize].u_k);
                (
                    Some(*u_k.last().expect("vector u_k must not be empty")),
                    Some(*u_k_plus_1.first().expect("vector u_k must not be empty"))
                )
            },
        };
        storage.u_right_minus.set(minus);
        storage.u_right_plus.set(plus);
    }
}

struct ElementStorage {
    // The derivative of r with respect to x, i.e. the metric of the x -> r mapping.
    r_x: Array,

    u_k: Array,
    // the interior value on the left face
    u_left_minus: Cell<Option<f32>>,
    // the exterior value on the left face
    u_left_plus: Cell<Option<f32>>,
    // the interior value on the right face
    u_right_minus: Cell<Option<f32>>,
    // the exterior value on the right face
    u_right_plus: Cell<Option<f32>>,
}

#[cfg(test)]
mod tests {
    use super::arrayfire::{print, Array, sin};
    use galerkin_1d::grid::{Element, ReferenceElement, Grid, Face, generate_grid};
    use galerkin_1d::advec::advec_1d;
    use galerkin_1d::operators::{Operators, assemble_operators};

    #[test]
    fn test() {
        let n_p = 8;
        let reference_element = ReferenceElement::legendre(n_p);
        let u_0 = |xs: &Array| sin(xs);
        let a = 3.4;
        let left_boundary_face = Face::BoundaryDirichlet(Box::new(|t: f32| t.sin()));
        let right_boundary_face = Face::Neumann(0.0);
        let grid: Grid = generate_grid(0.0, 4.0, 10, 3, &reference_element,
                                       left_boundary_face, right_boundary_face);

        let operators = assemble_operators(a, &grid, &reference_element);
        print(&operators.lift);

        advec_1d(u_0, &grid, &reference_element, &operators);
    }
}