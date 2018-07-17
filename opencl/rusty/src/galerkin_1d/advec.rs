extern crate arrayfire;

use galerkin_1d::grid::{Element, ReferenceElement, Grid, Face};
use std::num;
use std::cell::{Cell, RefCell, Ref, };
use self::arrayfire::{Array, Dim4, DType, Scalar, constant_t, print, matmul, mul, div, index, Convertable, MatProp, Seq};
use galerkin_1d::operators::{Operators, assemble_operators};
use std::fmt;
use functions::range_kutta::{RKA, RKB, RKC};
use std::ops::Deref;


pub fn advec_1d<Fx>(u_0: Fx, grid: &Grid, reference_element: &ReferenceElement,
                    operators: &Operators)
    where Fx: Fn(&Array) -> Array {
    let nt = 10;
    let dt: f32 = 5.9827e-04;
    let mut t: f32 = 0.0;

    let storage: Vec<ElementStorage> = initialize_storage(u_0, reference_element.n_p,
                                                              grid, operators);
    let mut residual_u = constant_t(Scalar::F32(0.),
                                    Dim4::new(&[reference_element.n_p as u64 + 1, 1, 1, 1]),
                                    DType::F32);
    for tstep in 0..1 {
        for int_rk in 0..2 {
            let t = t + RKC[int_rk] * dt;
            // communicate current values of u across faces
            println!("t = {}", t);
            communicate(t as f32, grid, reference_element.n_p, &storage);
            println!("{:?}", &storage);
            // update each element's local solution
            for elt in (*grid).elements.iter() {
                let storage = &storage[elt.index as usize];
                let rhs_u = advec_rhs_1d(&elt, &storage, &operators);
//                println!("rhsu:");
//                print(&rhs_u);
                residual_u = &residual_u * RKA[int_rk] + rhs_u * dt;
//                print(&residual_u);
                let new_u = {
                    let u_ref = storage.u_k.borrow();
                    u_ref.deref() + &residual_u * RKB[int_rk]
                };
                new_u.eval();
//                print(&new_u);
                storage.u_k.replace(new_u);
            }
        }
        for elt in (*grid).elements.iter() {
            let storage = &storage[elt.index as usize];
//            print(&storage.u_k.borrow().deref());
        }
        t = t + dt;
    }
}

fn advec_rhs_1d(elt: &Element, elt_storage: &ElementStorage, operators: &Operators) -> Array {
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
            (operators.a * u_h - numerical_flux) * elt.left_outward_normal
        }
    } as f32;
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
            if elt.index == 4 {
                println!("nf: {}", numerical_flux);
                println!("uh: {}", a * u_h);
                println!("diff: {}", numerical_flux - a * u_h);
            }
            (((a * u_h) - numerical_flux) * elt.right_outward_normal)
        }
    } as f32;
    let du: Array = Array::new(&[du_left, du_right], Dim4::new(&[2, 1, 1, 1]));
    println!("du:");
    print(&(&du * 1.0e5 as f32));
    let dr_u = matmul(&operators.d_r, elt_storage.u_k.borrow().deref(),
                      MatProp::NONE, MatProp::NONE);
    let a_rx = &elt_storage.r_x * (-operators.a);
//    println!("a_rx:");
//    print(&a_rx);
    let rhs_u = a_rx * dr_u;
//    println!("rhs_u:");
//    print(&rhs_u);
    let lifted_flux = matmul(&operators.lift, &(&elt_storage.r_x_at_faces * du),
                             MatProp::NONE, MatProp::NONE);
//    println!("lifted_flux:");
//    print(&(&lifted_flux * 1.0e7 as f32));
    let result = rhs_u + lifted_flux;
    result.eval();
    result
}

fn lax_friedrichs(operators: &Operators, u_minus: f32, u_plus: f32, outward_normal: f32) -> f32 {
    let f_a = u_minus * operators.a;
    let f_b = u_plus * operators.a;
    let avg = (f_a + f_b) / 2.;
    let jump = (u_minus * (-outward_normal)) + (u_plus * outward_normal);
    avg + operators.a.abs() * jump / 2.
}

fn initialize_storage<Fx>(u_0: Fx, n_p: i32, grid: &Grid, operators: &Operators) -> Vec<ElementStorage>
    where Fx: Fn(&Array) -> Array {
    grid.elements.iter().map(|elt| {
        let r_x = div(&(1.0 as f32),
                      &matmul(&operators.d_r, &elt.x_k, MatProp::NONE, MatProp::NONE), false);
        // want to extract the 0th and n_pth elements.
        let face_seqs = &[Seq::new(0, n_p, n_p)];
        let r_x_at_faces = index(&r_x, face_seqs);
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
        let mut u_k = array_to_vector(size, storage.u_k.borrow().deref());
        let (minus, plus) = match *elt.left_face {
            Face::Neumann(_) => (None, None),
            Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (
                    // minus is outside, plus is inside
                    Some(u_0),
                    Some(*u_k.first().expect("vector u_k must not be empty"))
                )
            }
            Face::Interior(j) => {
                let u_k_minus_1 = array_to_vector(size, storages[j as usize].u_k.borrow().deref());
                if i == 5 {
                    println!("{:?}", u_k_minus_1);
                }
                (
                    // minus is outside, plus is inside
                    Some(*u_k_minus_1.last().expect("vector u_k must not be empty")),
                    Some(*u_k.first().expect("vector u_k must not be empty"))
                )
            }
        };
        if i == 5 {
            println!("({:?}, {:?})", minus, plus);
        }
        storage.u_left_minus.set(minus);
        storage.u_left_plus.set(plus);

        let (minus, plus) = match *elt.right_face {
            Face::Neumann(_) => (None, None),
            Face::BoundaryDirichlet(ref u_0) => {
                let u_0 = u_0(t);
                (
                    // minus is outside, plus is inside
                    Some(u_0),
                    Some(*u_k.first().expect("vector u_k must not be empty"))
                )
            }
            Face::Interior(j) => {
                let u_k_plus_1 = array_to_vector(size, storages[j as usize].u_k.borrow().deref());
//                println!("j: {}, u_k+1: {:?}", j, &u_k_plus_1);
                if i == 4 {
                    println!("{:?}", u_k);
                }
                (
                    // minus is outside, plus is inside
                    Some(*u_k_plus_1.first().expect("vector u_k must not be empty")),
                    Some(*u_k.last().expect("vector u_k must not be empty"))
                )
            }
        };
        storage.u_right_minus.set(minus);
        storage.u_right_plus.set(plus);
    }
}

struct ElementStorage {
    // The derivative of r with respect to x, i.e. the metric of the x -> r mapping.
    r_x: Array,
    r_x_at_faces: Array,

    u_k: RefCell<Array>,
    // the interior value on the left face
    u_left_minus: Cell<Option<f32>>,
    // the exterior value on the left face
    u_left_plus: Cell<Option<f32>>,
    // the interior value on the right face
    u_right_minus: Cell<Option<f32>>,
    // the exterior value on the right face
    u_right_plus: Cell<Option<f32>>,
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

#[cfg(test)]
mod tests {
    use super::arrayfire::{print, Array, sin};
    use galerkin_1d::grid::{Element, ReferenceElement, Grid, Face, generate_grid};
    use galerkin_1d::advec::advec_1d;
    use galerkin_1d::operators::{Operators, assemble_operators};
    use std::f32::consts;

    #[test]
    fn test() {
        let n_p = 8;
        let reference_element = ReferenceElement::legendre(n_p);
        let u_0 = |xs: &Array| sin(xs);
        let a = consts::PI * 2.;
        let left_boundary_face = Face::BoundaryDirichlet(Box::new(move |t: f32| -(a * t).sin()));
        let right_boundary_face = Face::Neumann(0.0);
        let grid: Grid = generate_grid(0.0, 2.0, 10, 8, &reference_element,
                                       left_boundary_face, right_boundary_face);

        let operators = assemble_operators(a, &grid, &reference_element);
        print(&operators.lift);

        advec_1d(u_0, &grid, &reference_element, &operators);
    }
}