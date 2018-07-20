extern crate rulinalg;

use galerkin_1d::grid::{Element, ReferenceElement, Grid, Face, generate_grid};
use std::cell::{Cell, RefCell};
use self::rulinalg::vector::Vector;
use galerkin_1d::operators::Operators;
use galerkin_1d::operators::assemble_operators;
use std::fmt;
use functions::range_kutta::{RKA, RKB, RKC};
use std::ops::Deref;
use std::iter::repeat;
use std::f64::consts;

#[inline(never)]
pub fn advec_1d<Fx>(u_0: Fx, grid: &Grid, reference_element: &ReferenceElement,
                    operators: &Operators)
    where Fx: Fn(&Vector<f64>) -> Vector<f64> {

    let final_time = 100.3;

    let cfl = 0.75;
    let x_scale = 0.01;
    let dt: f64 = 0.5 * cfl / (consts::PI * 2.) * x_scale;
    let n_t = (final_time / dt).ceil() as i32;
    let dt = final_time / n_t as f64;


    let mut t: f64 = 0.0;

    let storage: Vec<ElementStorage> = initialize_storage(u_0, reference_element.n_p,
                                                          grid, operators);
    let mut residuals: Vec<Vector<f64>> = repeat(Vector::zeros(reference_element.n_p as usize + 1))
        .take(grid.elements.len())
        .collect();

    for _ in 0..n_t {
        for int_rk in 0..5 {
            let t = t + RKC[int_rk] * dt;

            // communicate current values of u across faces
            communicate(t, grid, reference_element.n_p, &storage);

            // update each element's local solution
            for elt in (*grid).elements.iter() {
                let storage = &storage[elt.index as usize];

                let residuals_u = {
                    let residuals_u = &(residuals[elt.index as usize]);

                    let rhs_u = advec_rhs_1d(&elt, &storage, &operators);
                    residuals_u * RKA[int_rk] + rhs_u * dt
                };

                let new_u = {
                    let u_ref = storage.u_k.borrow();
                    u_ref.deref() + &residuals_u * RKB[int_rk]
                };

                residuals[elt.index as usize] = residuals_u;

                storage.u_k.replace(new_u);
            }
        }
        t = t + dt;
    }
    for elt in (*grid).elements.iter() {
        let storage = &storage[elt.index as usize];
        println!("{:?}", &storage.u_k.borrow().deref());
    }
}

fn advec_rhs_1d(elt: &Element, elt_storage: &ElementStorage, operators: &Operators) -> Vector<f64> {
    let du_left = match *elt.left_face {
        Face::Neumann(f) => f,
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
        Face::Neumann(f) => f,
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
    let dr_u = &operators.d_r * elt_storage.u_k.borrow().deref();
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

fn initialize_storage<Fx>(u_0: Fx, n_p: i32, grid: &Grid, operators: &Operators)
                          -> Vec<ElementStorage>
    where Fx: Fn(&Vector<f64>) -> Vector<f64> {
    grid.elements.iter().map(|elt| {
        let d_r_x_k = &operators.d_r * &elt.x_k;
        let r_x = Vector::ones(d_r_x_k.size()).elediv(&d_r_x_k);
        let r_x_at_faces = vector![r_x[0], r_x[n_p as usize]];
        ElementStorage {
            r_x,
            r_x_at_faces,
            u_k: RefCell::new(u_0(&elt.x_k)),
            u_left_minus: Cell::new(None),
            u_right_minus: Cell::new(None),
            u_left_plus: Cell::new(None),
            u_right_plus: Cell::new(None),
        }
    }).collect()
}

// Pass flux information across faces into each element's local storage.
fn communicate(t: f64, grid: &Grid, n_p: i32, storages: &Vec<ElementStorage>) {
    for (i, elt) in grid.elements.iter().enumerate() {
        let storage = storages.get(i).expect("index mismatch");
        let borrow = storage.u_k.borrow();
        let mut u_k = borrow.deref();
        let (minus, plus) = match *elt.left_face {
            Face::Neumann(_) => (None, None),
            Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (
                    // minus is outside, plus is inside
                    Some(u_0),
                    Some(u_k[0])
                )
            }
            Face::Interior(j) => {
                let borrow = storages[j as usize].u_k.borrow();
                let u_k_minus_1 = borrow.deref();
                (
                    // minus is outside, plus is inside
                    Some(u_k_minus_1[u_k_minus_1.size() - 1]),
                    Some(u_k[0])
                )
            }
        };
        storage.u_left_minus.set(minus);
        storage.u_left_plus.set(plus);

        let (minus, plus) = match *elt.right_face {
            Face::Neumann(_) => (None, None),
            Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (
                    // minus is outside, plus is inside
                    Some(u_0),
                    Some(u_k[0])
                )
            }
            Face::Interior(j) => {
                let borrow = storages[j as usize].u_k.borrow();
                let u_k_plus_1 = borrow.deref();
                (
                    // minus is outside, plus is inside
                    Some(u_k_plus_1[0]),
                    Some(u_k[u_k.size() - 1])
                )
            }
        };
        storage.u_right_minus.set(minus);
        storage.u_right_plus.set(plus);
    }
}

struct ElementStorage {
    // The derivative of r with respect to x, i.e. the metric of the x -> r mapping.
    r_x: Vector<f64>,
    r_x_at_faces: Vector<f64>,

    u_k: RefCell<Vector<f64>>,
    // the interior value on the left face
    u_left_minus: Cell<Option<f64>>,
    // the exterior value on the left face
    u_left_plus: Cell<Option<f64>>,
    // the interior value on the right face
    u_right_minus: Cell<Option<f64>>,
    // the exterior value on the right face
    u_right_plus: Cell<Option<f64>>,
}

impl fmt::Debug for ElementStorage {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{{\n")?;
        write!(f, "\tu_left_minus: {:?},\n", self.u_left_minus)?;
        write!(f, "\tu_left_plus: {:?},\n", self.u_left_plus)?;
        write!(f, "\tu_right_minus: {:?},\n", self.u_right_minus)?;
        write!(f, "\tu_right_plus: {:?},\n", self.u_right_minus)?;
        write!(f, "}}")
    }
}

fn u_0(xs: &Vector<f64>) -> Vector<f64> {
    xs.iter().map(|x: &f64| x.sin()).collect()
}

pub fn advec_1d_example() {
    let n_p = 8;
    let reference_element = ReferenceElement::legendre(n_p);
    let a = consts::PI * 2.;
    let left_boundary_face = Face::BoundaryDirichlet(Box::new(move |t: f64| -(a * t).sin()));
    let right_boundary_face = Face::Neumann(0.0);
    let grid: Grid = generate_grid(0.0, 2.0, 10, 8, &reference_element,
                                   left_boundary_face, right_boundary_face);

    let operators = assemble_operators(a, &grid, &reference_element);

    advec_1d(&u_0, &grid, &reference_element, &operators);
}

#[cfg(test)]
mod tests {
    use super::rulinalg::vector::Vector;
    use galerkin_1d::grid::{ReferenceElement, Grid, Face, generate_grid};
    use galerkin_1d::advec::{advec_1d_example};
    use galerkin_1d::operators::assemble_operators;
    use std::f64::consts;

    #[test]
    fn test() {
        advec_1d_example();
    }
}