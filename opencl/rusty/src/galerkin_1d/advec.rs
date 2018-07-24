extern crate rulinalg;

use galerkin_1d::grid::{ReferenceElement, generate_grid};
use galerkin_1d::grid;
use std::cell::{Cell, RefCell};
use self::rulinalg::vector::Vector;
use galerkin_1d::operators::Operators;
use galerkin_1d::operators::assemble_operators;
use std::fmt;
use functions::range_kutta::{RKA, RKB, RKC};
use std::ops::Deref;
use std::iter::repeat;
use std::f64::consts;
use galerkin_1d::unknowns::{Unknown, ElementStorage, initialize_storage, communicate};

#[inline(never)]
pub fn advec_1d<Fx>(u_0: Fx, grid: &Grid, reference_element: &ReferenceElement,
                    operators: &Operators)
    where Fx: Fn(&Vector<f64>) -> U {
    let final_time = 1.3;

    let cfl = 0.75;
    let x_scale = 0.01;
    let dt: f64 = 0.5 * cfl / (consts::PI * 2.) * x_scale;
    let n_t = (final_time / dt).ceil() as i32;
    let dt = final_time / n_t as f64;

    let mut t: f64 = 0.0;

    let mut storage: Vec<UStorage> = initialize_storage(u_0, reference_element.n_p,
                                                    grid, operators);
    let mut residuals: Vec<Vector<f64>> = repeat(Vector::zeros(reference_element.n_p as usize + 1))
        .take(grid.elements.len())
        .collect();

    for _ in 0..n_t {
        for int_rk in 0..5 {
            let t = t + RKC[int_rk] * dt;

            // communicate current values of u across faces
            communicate(t, grid, &storage);

            // update each element's local solution
            for elt in (*grid).elements.iter() {
                let mut storage = &mut storage[elt.index as usize];

                let residuals_u = {
                    let residuals_u = &(residuals[elt.index as usize]);

                    let rhs_u = advec_rhs_1d(&elt, &storage, &operators);
                    residuals_u * RKA[int_rk] + rhs_u * dt
                };

                let new_u = {
                    let u_ref: &U = &storage.u_k;
                    &u_ref.u + &residuals_u * RKB[int_rk]
                };

                residuals[elt.index as usize] = residuals_u;

                storage.u_k = U { u: new_u };
            }
        }
        t = t + dt;
    }
    for elt in (*grid).elements.iter() {
        let storage = &storage[elt.index as usize];
        println!("{:?}", &storage.u_k);
    }
}

fn advec_rhs_1d(elt: &Element, elt_storage: &UStorage, operators: &Operators) -> Vector<f64> {
    let du_left = match *elt.left_face {
        grid::Face::Neumann(f) => f,
        _ => {
            let u_h = elt_storage.u_left_plus.get()
                .expect("Non-Neumann face should have populated u_plus");
            let numerical_flux = lax_friedrichs(
                operators,
                elt_storage.u_left_minus.get()
                    .expect("Non-Neumann face should have populated u_minus"),
                u_h,
                elt.left_outward_normal,
            );
            let a = operators.a;
            (((a * u_h) - numerical_flux) * elt.left_outward_normal)
        }
    };
    let du_right = match *elt.right_face {
        grid::Face::Neumann(f) => f,
        _ => {
            let u_h = elt_storage.u_right_plus.get()
                .expect("Non-Neumann face should have populated u_plus");
            let numerical_flux = lax_friedrichs(
                operators,
                elt_storage.u_right_minus.get()
                    .expect("Non-Neumann face should have populated u_minus"),
                u_h,
                elt.right_outward_normal,
            );
            let a = operators.a;
            (((a * u_h) - numerical_flux) * elt.right_outward_normal)
        }
    };
    let du: Vector<f64> = vector![du_left, du_right];
    let dr_u = &operators.d_r * &elt_storage.u_k.u;
    let a_rx = &elt_storage.r_x * (-operators.a);
    let rhs_u = &a_rx.elemul(&dr_u);
    let scaled_du = &elt_storage.r_x_at_faces.elemul(&du);
    let lifted_flux = &operators.lift * scaled_du;
    let result = rhs_u + lifted_flux;
    result
}

fn lax_friedrichs(operators: &Operators, u_minus: f64, u_plus: f64, outward_normal: f64) -> f64 {
    let f_a = u_minus * operators.a;
    let f_b = u_plus * operators.a;
    let avg = (f_a + f_b) / 2.;
    let jump = (u_minus * (-outward_normal)) + (u_plus * outward_normal);
    avg + operators.a.abs() * jump / 2.
}

#[derive(Debug)]
pub struct U {
    u: Vector<f64>,
}

impl Unknown for U {
    type Unit = f64;

    fn first(&self) -> f64 {
        self.u[0]
    }

    fn last(&self) -> f64 {
        self.u[self.u.size() - 1]
    }
}

type UStorage = ElementStorage<U>;

type Grid = grid::Grid<U>;

type Element = grid::Element<U>;

fn u_0(xs: &Vector<f64>) -> U {
    U { u: xs.iter().map(|x: &f64| x.sin()).collect() }
}

pub fn advec_1d_example() {
    let n_p = 8;
    let reference_element = ReferenceElement::legendre(n_p);
    let a = consts::PI * 2.;
    let left_boundary_face = grid::Face::BoundaryDirichlet(Box::new(move |t: f64| -(a * t).sin()));
    let right_boundary_face = grid::Face::Neumann(0.0);
    let grid: Grid = generate_grid(0.0, 2.0, 10, &reference_element,
                                   left_boundary_face, right_boundary_face);

    let operators = assemble_operators::<U>(a, &reference_element);

    advec_1d(&u_0, &grid, &reference_element, &operators);
}

#[cfg(test)]
mod tests {
    use super::rulinalg::vector::Vector;
    use galerkin_1d::grid::{ReferenceElement, Grid, Face, generate_grid};
    use galerkin_1d::advec::advec_1d_example;
    use galerkin_1d::operators::assemble_operators;
    use std::f64::consts;

    #[test]
    fn test() {
        advec_1d_example();
    }
}