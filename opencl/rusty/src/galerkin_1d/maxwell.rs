extern crate core;
extern crate rulinalg;

use self::core::ops::{Add, Div, Mul, Neg};
use functions::range_kutta::{RKA, RKB, RKC};
use galerkin_1d::flux::FluxEnum;
use galerkin_1d::flux::FluxScheme;
use galerkin_1d::flux::NumericalFlux;
use galerkin_1d::flux::Side;
use galerkin_1d::galerkin::compute_flux;
use galerkin_1d::galerkin::Formulation;
use galerkin_1d::galerkin::GalerkinScheme;
use galerkin_1d::grid;
use galerkin_1d::grid::FaceType;
use galerkin_1d::operators::{assemble_operators, Operators};
use galerkin_1d::unknowns::{communicate, initialize_storage, Unknown};
use rulinalg::vector::Vector;
use std::f64::consts;
use std::iter::repeat;

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

impl Add for EHUnit {
    type Output = EHUnit;

    fn add(self, other: EHUnit) -> EHUnit {
        EHUnit {
            E: self.E + other.E,
            H: self.H + other.H,
        }
    }
}

impl Mul<f64> for EHUnit {
    type Output = EHUnit;

    fn mul(self, other: f64) -> EHUnit {
        EHUnit {
            E: self.E * other,
            H: self.H * other,
        }
    }
}

impl Div<f64> for EHUnit {
    type Output = EHUnit;

    fn div(self, other: f64) -> EHUnit {
        EHUnit {
            E: self.E / other,
            H: self.H / other,
        }
    }
}

impl Neg for EHUnit {
    type Output = EHUnit;

    fn neg(self) -> EHUnit {
        EHUnit {
            E: -self.E,
            H: -self.H,
        }
    }
}

impl Unknown for EH {
    type Unit = EHUnit;

    fn first(&self) -> Self::Unit {
        EHUnit {
            E: self.E[0],
            H: self.H[0],
        }
    }

    fn last(&self) -> Self::Unit {
        EHUnit {
            E: self.E[self.E.size() - 1],
            H: self.H[self.H.size() - 1],
        }
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
        Permittivity {
            epsilon: 0.0,
            mu: 0.0,
        }
    }
}

type Grid = grid::Grid<Maxwells>;

type Element = grid::Element<Maxwells>;

type EHStorage = grid::ElementStorage<EH, Permittivity>;

fn permittivityFlux(
    de: f64,
    dh: f64,
    f_minus: Permittivity,
    f_plus: Permittivity,
    outward_normal: f64,
) -> EHUnit {
    let (z_minus, z_plus) = (
        (f_minus.mu / f_minus.epsilon).sqrt(),
        (f_plus.mu / f_plus.epsilon).sqrt(),
    );
    let (y_minus, y_plus) = (1. / z_minus, 1. / z_plus);
    EHUnit {
        E: (1. / (y_minus + y_plus)) * (outward_normal * y_plus * de - dh),
        H: (1. / (z_minus + z_plus)) * (outward_normal * z_plus * dh - de),
    }
}

#[derive(Copy, Clone)]
struct MaxwellsInteriorFlux {}

impl NumericalFlux<EH, Permittivity> for MaxwellsInteriorFlux {
    fn flux(
        &self,
        minus: Side<EH, Permittivity>,
        plus: Side<EH, Permittivity>,
        outward_normal: f64,
    ) -> EHUnit {
        let (de, dh) = (minus.u.E - plus.u.E, minus.u.H - plus.u.H);
        permittivityFlux(de, dh, minus.f, plus.f, outward_normal)
    }
}

#[derive(Copy, Clone)]
struct MaxwellsExteriorFlux {}

impl NumericalFlux<EH, Permittivity> for MaxwellsExteriorFlux {
    fn flux(
        &self,
        minus: Side<EH, Permittivity>,
        plus: Side<EH, Permittivity>,
        outward_normal: f64,
    ) -> EHUnit {
        let (de, dh) = (2. * minus.u.E, 0.);
        permittivityFlux(de, dh, minus.f, plus.f, outward_normal)
    }
}

struct MaxwellsFluxScheme {}

impl FluxScheme<EH, Permittivity> for MaxwellsFluxScheme {
    type Left = MaxwellsExteriorFlux;
    type Right = MaxwellsExteriorFlux;
    type Interior = MaxwellsInteriorFlux;
}

struct Maxwells {}

impl GalerkinScheme for Maxwells {
    type U = EH;
    type F = Permittivity;
    type FS = MaxwellsFluxScheme;

    const FORMULATION: Formulation = Formulation::Strong;
}

fn permittivity(xs: &Vector<f64>) -> Permittivity {
    if xs[0] >= 0.0 {
        Permittivity {
            epsilon: 2.0,
            mu: 1.0,
        }
    } else {
        Permittivity {
            epsilon: 1.0,
            mu: 1.0,
        }
    }
}

fn eh_0(xs: &Vector<f64>) -> EH {
    EH {
        E: xs
            .iter()
            .map(|x: &f64| if *x < 0. { (consts::PI * x).sin() } else { 0. })
            .collect(),
        H: Vector::zeros(xs.size()),
    }
}

pub fn maxwell_1d_example() {
    let n_p = 10;
    let reference_element = grid::ReferenceElement::legendre(n_p);
    let left_boundary_face = grid::Face {
        face_type: FaceType::Boundary(
            Box::new(move |_: f64, other_side: EHUnit| EHUnit {
                E: 0.0,
                H: other_side.H,
            }),
            Permittivity {
                epsilon: 1.0,
                mu: 1.0,
            },
        ),
        flux: FluxEnum::Left(MaxwellsExteriorFlux {}),
    };
    let right_boundary_face = grid::Face {
        face_type: FaceType::Boundary(
            Box::new(move |_: f64, other_side: EHUnit| EHUnit {
                E: 0.0,
                H: other_side.H,
            }),
            Permittivity {
                epsilon: 2.0,
                mu: 1.0,
            },
        ),
        flux: FluxEnum::Right(MaxwellsExteriorFlux {}),
    };
    let grid: grid::Grid<Maxwells> = grid::generate_grid(
        -1.0,
        1.0,
        8,
        &reference_element,
        left_boundary_face,
        right_boundary_face,
        MaxwellsInteriorFlux {},
        &permittivity,
    );
    let operators = assemble_operators::<EH>(&reference_element);

    maxwell_1d(&eh_0, &grid, &reference_element, &operators);
}

fn maxwell_1d<Fx>(
    eh_0: Fx,
    grid: &Grid,
    reference_element: &grid::ReferenceElement,
    operators: &Operators,
) where
    Fx: Fn(&Vector<f64>) -> EH,
{
    //    let mut plotter = Plotter::create(-1.0, 1.0, -1.0, 1.0);

    let final_time = 200.0;
    let cfl = 0.75;
    let x_scale = 0.01;
    let dt: f64 = 0.5 * cfl / (consts::PI * 2.) * x_scale;
    let n_t = (final_time / dt).ceil() as i32;
    let dt = final_time / n_t as f64;

    let mut t: f64 = 0.0;

    let mut storage: Vec<EHStorage> =
        initialize_storage(eh_0, reference_element.n_p, grid, operators);
    let mut residuals: Vec<(Vector<f64>, Vector<f64>)> = repeat((
        Vector::zeros(reference_element.n_p as usize + 1),
        Vector::zeros(reference_element.n_p as usize + 1),
    )).take(grid.elements.len())
        .collect();

    for epoch in 0..n_t {
        for int_rk in 0..5 {
            let t = t + RKC[int_rk] * dt;

            communicate(t, grid, &storage);

            for elt in (*grid).elements.iter() {
                let mut storage = &mut storage[elt.index as usize];

                let (residuals_e, residuals_h) = {
                    let (residuals_e, residuals_h) = &(residuals[elt.index as usize]);
                    let (rhs_e, rhs_h) =
                        maxwell_rhs_1d(grid.elements.len() as i32, &elt, &storage, &operators);
                    (
                        residuals_e * RKA[int_rk] + rhs_e * dt,
                        residuals_h * RKA[int_rk] + rhs_h * dt,
                    )
                };

                let new_eh = {
                    let eh_ref: &EH = &storage.u_k;
                    EH {
                        E: &eh_ref.E + &residuals_e * RKB[int_rk],
                        H: &eh_ref.H + &residuals_h * RKB[int_rk],
                    }
                };

                residuals[elt.index as usize] = (residuals_e, residuals_h);
                storage.u_k = new_eh;
            }
        }
        if epoch % 20 == 0 {
            //            plotter.header();
            //            for elt in (*grid).elements.iter() {
            //                let storage = &storage[elt.index as usize];
            //                plotter.plot(&elt.x_k, &storage.u_k.E);
            //            }
            //            plotter.replot();
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

fn maxwell_rhs_1d(
    n_k: i32,
    elt: &Element,
    elt_storage: &EHStorage,
    operators: &Operators,
) -> (Vector<f64>, Vector<f64>) {
    let (flux_left, flux_right) = compute_flux(elt, elt_storage);

    let dr_h = &operators.d_r * &elt_storage.u_k.H;
    let flux_h = vector![flux_left.H, flux_right.H];
    let lifted_flux_h = &operators.lift * &elt_storage.r_x_at_faces.elemul(&flux_h);
    let rhs_e = ((&elt_storage.r_x * -1.).elemul(&dr_h) + lifted_flux_h) / elt.spatial_flux.epsilon;

    let dr_e = &operators.d_r * &elt_storage.u_k.E;
    let flux_e = vector![flux_left.E, flux_right.E];
    let lifted_flux_e = &operators.lift * &elt_storage.r_x_at_faces.elemul(&flux_e);
    let rhs_h = ((&elt_storage.r_x * -1.).elemul(&dr_e) + lifted_flux_e) / elt.spatial_flux.mu;

    (rhs_e, rhs_h)
}

#[cfg(test)]
mod tests {
    use galerkin_1d::maxwell::maxwell_1d_example;

    #[test]
    fn test() {
        maxwell_1d_example();
    }
}
