extern crate rulinalg;

use functions::range_kutta::{RKA, RKB, RKC};
use galerkin_1d::unknowns::{Unknown, communicate, initialize_storage};
use rulinalg::vector::{Vector};
use galerkin_1d::grid;
use galerkin_1d::operators::{Operators, assemble_operators};
use std::iter::repeat;
use std::f64::consts;
use std::cell::Cell;

#[derive(Debug)]
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

#[derive(Copy, Clone)]
struct Permittivity {
    epsilon: f64,
    mu: f64,
}

impl grid::SpatialFlux for Permittivity {
    type Unit = Self;

    fn first(&self) -> Self::Unit {
        *self
    }

    fn last(&self) -> Self::Unit {
        *self
    }

    fn zero() -> Self::Unit {
        Permittivity { epsilon: 0.0, mu: 0.0 }
    }
}

type Grid = grid::Grid<EH, Permittivity>;

type Element = grid::Element<EH, Permittivity>;

type EHStorage = grid::ElementStorage<EH, Permittivity>;

fn permittivity(xs: &Vector<f64>) -> Permittivity {
    if xs[0] >= 0.0 {
        Permittivity { epsilon: 2.0, mu: 1.0 }
    } else {
        Permittivity { epsilon: 1.0, mu: 1.0 }
    }
}

fn eh_0(xs: &Vector<f64>) -> EH {
    EH {
        E: xs.iter().map(|x: &f64| if *x < 0. { (consts::PI * x).sin() } else { 0. }).collect(),
        H: Vector::zeros(xs.size()),
    }
}

pub fn maxwell_1d_example() {
    let n_p = 6;
    let reference_element = grid::ReferenceElement::legendre(n_p);
    let left_boundary_face = grid::Face::Boundary(Box::new(move |_: f64, other_side: EHUnit|
        EHUnit { E: 0.0, H: other_side.H }
    ), Permittivity { epsilon: 1.0, mu: 1.0 });
    let right_boundary_face = grid::Face::Boundary(Box::new(move |_: f64, other_side: EHUnit|
        EHUnit { E: 0.0, H: other_side.H }
    ), Permittivity { epsilon: 2.0, mu: 1.0 });
    let grid: grid::Grid<EH, Permittivity> = grid::generate_grid(
        -1.0, 1.0, 8, &reference_element, left_boundary_face,
        right_boundary_face, &permittivity);
    let operators = assemble_operators::<EH>(&reference_element);

    maxwell_1d(&eh_0, &grid, &reference_element, &operators);
}

fn maxwell_1d<Fx>(eh_0: Fx, grid: &Grid, reference_element: &grid::ReferenceElement,
                  operators: &Operators)
    where Fx: Fn(&Vector<f64>) -> EH {
    let final_time = 4.5;
    let cfl = 0.75;
    let x_scale = 0.01;
//    let dt: f64 = 0.5 * cfl / (consts::PI * 2.) * x_scale;
    let dt: f64 = 0.021186440677966;
    let n_t = (final_time / dt).ceil() as i32;
    let dt = final_time / n_t as f64;
    println!("here");

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

    println!("here");
    for _ in 0..1 {
        for int_rk in 0..1 {
            let t = t + RKC[int_rk] * dt;
            println!("\nt: {}", t);

            communicate(t, grid, &storage);

            for elt in (*grid).elements.iter() {
                let mut storage = &mut storage[elt.index as usize];

                let (residuals_e, residuals_h) = {
                    let (residuals_e, residuals_h) = &(residuals[elt.index as usize]);
                    let (rhs_e, rhs_h) = maxwell_rhs_1d(&elt, &storage, &operators);
                    println!("rhs_e: {:?}", rhs_e);
                    println!("rhs_h: {:?}", rhs_h);
                    (
                        residuals_e * RKA[int_rk] + rhs_e * dt,
                        residuals_h * RKA[int_rk] + rhs_h * dt
                    )
                };
                println!("res_e: {:?}", &residuals_e);
                println!("res_h: {:?}", &residuals_h);

                let new_eh = {
                    let eh_ref: &EH = &storage.u_k;
                    EH {
                        E: &eh_ref.E + &residuals_e * RKB[int_rk],
                        H: &eh_ref.H + &residuals_h * RKB[int_rk],
                    }
                };

                residuals[elt.index as usize] = (residuals_e, residuals_h);
                println!("EH: {:?}", &new_eh);
                storage.u_k = new_eh;
            }
        }
        t = t + dt;
    }
    println!("here");
    for elt in (*grid).elements.iter() {
        let storage = &storage[elt.index as usize];
        println!("{:?}", &storage.u_k.E);
    }
    println!("H:");
    for elt in (*grid).elements.iter() {
        let storage = &storage[elt.index as usize];
        println!("{:?}", &storage.u_k.H);
    }
}

fn maxwell_rhs_1d(elt: &Element, elt_storage: &EHStorage,
                  operators: &Operators) -> (Vector<f64>, Vector<f64>) {
    let (flux_e_left, flux_h_left) = {
        let outward_normal = elt.left_outward_normal;
        let (de, dh) = de_dh(elt_storage.u_left_plus.get(), elt_storage.u_left_minus.get(),
                             outward_normal);
        let (de, dh) = if elt.index == 0 { (de * 2., dh) } else { (de, dh) };
        println!("de: {}, dh: {}", de, dh);
        let (z_minus, z_plus) = z(elt_storage.f_left_minus.get(), elt_storage.f_left_plus.get());
        let y_minus = 1. / z_minus;
        let y_plus = 1. / z_plus;
        (
            (1. / (y_minus + y_plus)) * (outward_normal * y_plus * de - dh),
            (1. / (z_minus + z_plus)) * (outward_normal * z_plus * dh - de)
        )
    };
    let (flux_e_right, flux_h_right) = {
        let outward_normal = elt.right_outward_normal;
        let (de, dh) = de_dh(elt_storage.u_right_plus.get(), elt_storage.u_right_minus.get(),
                             outward_normal);
        let (z_minus, z_plus) = z(elt_storage.f_right_minus.get(), elt_storage.f_right_plus.get());
        let y_minus = 1. / z_minus;
        let y_plus = 1. / z_plus;
        (
            (1. / (y_minus + y_plus)) * (outward_normal * y_plus * de - dh),
            (1. / (z_minus + z_plus)) * (outward_normal * z_plus * dh - de)
        )
    };
    let dr_h = &operators.d_r * &elt_storage.u_k.H;
    let flux_h = vector![flux_h_left, flux_h_right];
    println!("flux_h: {:?}", flux_h);
    let lifted_flux_h = &operators.lift * &elt_storage.r_x_at_faces.elemul(&flux_h);
    let rhs_e = ((&elt_storage.r_x * -1.).elemul(&dr_h) + lifted_flux_h)
        / elt.spatial_flux.epsilon;

    let dr_e = &operators.d_r * &elt_storage.u_k.E;
    let flux_e = vector![flux_e_left, flux_e_right];
    println!("flux_e: {:?}", flux_e);
    let lifted_flux_e = &operators.lift * &elt_storage.r_x_at_faces.elemul(&flux_e);
    let rhs_h = ((&elt_storage.r_x * -1.).elemul(&dr_e) + lifted_flux_e)
        / elt.spatial_flux.mu;

    (rhs_e, rhs_h)
}

// Returns ([[E]], [[H]])
fn de_dh(eh_plus: EHUnit, eh_minus: EHUnit, outward_normal: f64) -> (f64, f64) {
    (
        eh_plus.E * -outward_normal + eh_minus.E * outward_normal,
        eh_plus.H * -outward_normal + eh_minus.H * outward_normal,
    )
}

fn z(permittivity_minus: Permittivity, permittivity_plus: Permittivity) -> (f64, f64) {
    (
        (permittivity_minus.mu / permittivity_minus.epsilon).sqrt(),
        (permittivity_plus.mu / permittivity_plus.epsilon).sqrt(),
    )
}

#[cfg(test)]
mod tests {
    use galerkin_1d::maxwell::maxwell_1d_example;

    #[test]
    fn test() {
        maxwell_1d_example();
    }
}