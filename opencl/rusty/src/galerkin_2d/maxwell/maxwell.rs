extern crate rulinalg;

use std::f64::consts;
use distmesh::distmesh_2d::unit_square;
use galerkin_2d::flux::compute_flux;
use galerkin_2d::galerkin::GalerkinScheme;
use galerkin_2d::grid::Element;
use galerkin_2d::grid::{assemble_grid, Grid, SpatialVariable};
use galerkin_2d::maxwell::flux::*;
use galerkin_2d::maxwell::unknowns::*;
use galerkin_2d::operators::curl_2d;
use galerkin_2d::operators::grad;
use galerkin_2d::operators::{assemble_operators, Operators};
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::unknowns::{communicate, initialize_storage, Unknown};
use rulinalg::vector::Vector;
use std::iter::repeat_with;
use galerkin_2d::grid::ElementStorage;
use galerkin_2d::operators::FaceLiftable;
use functions::range_kutta::RKA;
use functions::range_kutta::RKB;
use plot::plot3d::{Plotter3D, GnuplotPlotter3D};

#[derive(Debug)]
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
    Fx: Fn(&Vector<f64>, &Vector<f64>) -> EH,
{
    let mut plotter = GnuplotPlotter3D::create(-1., 1., -1., 1., -1., 1.);

    let final_time = 10.0;
    let dt: f64 = 0.003668181816046;
    let n_t = (final_time / dt).ceil() as i32;

    let mut t: f64 = 0.0;

    let mut storage: Vec<ElementStorage<Maxwell2D>> = initialize_storage(
        u_0,
        reference_element.n_p as i32,
        reference_element,
        grid,
        operators,
    );

    let mut residuals: Vec<EH> = repeat_with(|| EH::zero(reference_element))
        .take(grid.elements.len())
        .collect();

    for epoch in 0..n_t {
        for int_rk in 0..5 {
            communicate(t, reference_element, grid, &mut storage);

            for elt in (*grid).elements.iter() {
                let mut storage = &mut storage[elt.index as usize];

                let residuals_eh = {
                    let residuals_eh = &(residuals[elt.index as usize]);
                    let rhs = maxwell_rhs_2d(&elt, &storage, &operators, reference_element);
//                    println!("{:?}", rhs);
                    residuals_eh * RKA[int_rk] + rhs * dt
                };

                let eh = {
                    let eh: &EH = &storage.u_k;
                    eh + &(&residuals_eh * RKB[int_rk])
                };

                residuals[elt.index as usize] = residuals_eh;
                storage.u_k = eh;
            }
        }
        println!("epoch: {}", epoch);
        t = t + dt;
        if epoch % 20 == 0 {
            plotter.header();
            for elt in (*grid).elements.iter() {
                let storage = &storage[elt.index as usize];
                plotter.plot(&elt.x_k, &elt.y_k, &storage.u_k.Ez);
//                println!("{}", &storage.u_k.Hx);
            }
            plotter.replot();
        }
    }
}

fn maxwell_rhs_2d<'grid>(
    elt: &EHElement<'grid>,
    elt_storage: &ElementStorage<Maxwell2D>,
    operators: &Operators,
    reference_element: &ReferenceElement,
) -> EH {
    let (face1_flux, face2_flux, face3_flux) = compute_flux(elt, elt_storage);

    let flux = EH::lift_faces(
        &operators.lift,
        &(face1_flux * &elt.face1.f_scale),
        &(face2_flux * &elt.face2.f_scale),
        &(face3_flux * &elt.face3.f_scale),
    );

    if elt.index == 0 {
//        println!("flux: {}", flux);
//        println!("current_value: {}", elt_storage.u_k);
    }


//    println!("{:?}", elt.local_metric.jacobian);

    let grad_ez = grad(
        &elt_storage.u_k.Ez,
        operators,
        &elt.local_metric,
    );
    let curl_h = curl_2d(
        &elt_storage.u_k.Hx,
        &elt_storage.u_k.Hy,
        operators,
        &elt.local_metric,
    );

    let Hx = -grad_ez.y + flux.Hx / 2.0;
    let Hy = grad_ez.x + flux.Hy / 2.0;
    let Ez = curl_h + flux.Ez / 2.0;

    EH {
        Hx,
        Hy,
        Ez,
    }
}

pub fn maxwell_2d_example() {
    let n_p = 10;
    let reference_element = ReferenceElement::legendre(n_p);
    let operators = assemble_operators(&reference_element);
    let mesh = unit_square();
    let boundary_condition = |t| EH::face1_zero(&reference_element);
    let grid: Grid<Maxwell2D> = assemble_grid(
        &reference_element,
        &operators,
        &mesh,
        &boundary_condition,
        &|| (),
        |_, _| (),
        MaxwellFluxType::Interior,
        MaxwellFluxType::Exterior,
    );

//    println!("{}", operators.lift);
    maxwell_2d(&grid, &reference_element, &operators, &exact_cavity_solution_eh0);
}

fn exact_cavity_solution_eh0(xs: &Vector<f64>, ys: &Vector<f64>) -> EH {
    let pi = consts::PI;
    let omega = pi * consts::SQRT_2;
    let t = 0.;
    let Hx: Vector<f64> = xs.iter().zip(ys.iter()).map(|(&x, &y)| {
        -pi / omega * (pi * x).sin() * (pi * y).cos() * (omega * t).sin()
    }).collect();
    let Hy: Vector<f64> = xs.iter().zip(ys.iter()).map(|(&x, &y)| {
        pi / omega * (pi * x).cos() * (pi * y).sin() * (omega * t).sin()
    }).collect();
    let Ez: Vector<f64> = xs.iter().zip(ys.iter()).map(|(&x, &y)| {
        (pi * x).sin() * (pi * y).sin() * (omega * t).cos()
    }).collect();

    EH {
        Hx,
        Hy,
        Ez,
    }
}

#[cfg(test)]
mod tests {
    use super::maxwell_2d_example;

    #[test]
    pub fn test_maxwell_2d() {
        maxwell_2d_example();
    }
}
