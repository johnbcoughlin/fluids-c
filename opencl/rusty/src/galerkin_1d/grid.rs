use std::fmt;
use functions::jacobi_polynomials::grad_legendre_roots;

pub enum Flux {
    // The *flux* is constantly 0
    Zero,

    // A Dirichlet boundary condition which depends on time.
    // The first parameter is alpha, the Lax-Friedrichs parameter.
    BoundaryTimeDependent(f64, Box<Fn(f64) -> f64>),

    // The flux parameter alpha
    LaxFriedrichs(f64),
}

impl fmt::Debug for Flux {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match *self {
            Flux::Zero => write!(f, "Zero"),
            Flux::BoundaryTimeDependent(alpha, _) => write!(f, "BoundaryTimeDependent({})", alpha),
            Flux::LaxFriedrichs(alpha) => write!(f, "LaxFriedrichs({})", alpha),
        }
    }
}

#[derive(Debug)]
pub struct Element<'flux> {
    pub index: i32,
    pub x_left: f64,
    pub x_right: f64,

    // A vector of values of the solution, u, at the interpolation points r_i.
    // Refer to ReferenceElement for the interpolation points.
    pub u_k: Vec<f64>,

    pub left_flux: &'flux Flux,
    pub right_flux: &'flux Flux,
}

impl<'flux> fmt::Display for Element<'flux> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "D_{}: [{:.2}, {:.2}]", self.index, self.x_left, self.x_right)
    }
}

#[derive(Debug)]
pub struct ReferenceElement {
    // The order of polynomial approximation N_p
    n_p: i32,

    // The vector of interpolation points in the reference element [-1, 1].
    // The first value in this vector is -1, and the last is 1.
    rs: Vec<f64>,
}

impl ReferenceElement {
    pub fn legendre(n_p: i32) -> ReferenceElement {
        let mut rs = vec![-1.];
        let mut h: Vec<f32> = vec![0.; n_p as usize - 1];
        let roots = grad_legendre_roots(n_p);
        roots.eval();
        roots.host(&mut h);
        println!("{:?}", h);
        for &r in h.iter() {
            rs.push(r as f64);
        }
        rs.push(1.);
        ReferenceElement { n_p, rs }
    }
}

#[derive(Debug)]
pub struct Grid<'flux> {
    pub elements: Vec<Element<'flux>>,
}

impl<'f> fmt::Display for Grid<'f> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        let elts = &self.elements;
        write!(f, "[ ")?;
        for (count, elt) in elts.iter().enumerate() {
            if count != 0 { write!(f, ", ")?; }
            write!(f, "{}\n", elt)?;
        }
        write!(f, "]")
    }
}

pub fn generate_grid<'flux, Fx>(x_min: f64, x_max: f64, n_k: i32, n_p: i32,
                                reference_element: &ReferenceElement, u_0: Fx,
                                left_boundary_flux: &'flux Flux,
                                right_boundary_flux: &'flux Flux,
                                internal_flux: &'flux Flux, ) -> Grid<'flux>
    where Fx: Fn(f64) -> f64 {

    assert!(x_max > x_min);
    let diff = (x_max - x_min) / (n_k as f64);
    let elements = (0..n_k).map(|k| {
        let left = x_min + diff * (k as f64);
        Element {
            index: k,
            x_left: left,
            x_right: left + diff,
            u_k: reference_element.rs.iter().map(|r| {
                let s = (r + 1.) / 2.;
                let x = left + diff * s;
                u_0(x)
            }).collect(),
            left_flux: if k == 0 {
                left_boundary_flux
            } else {
                internal_flux
            },
            right_flux: if k < n_k - 1 {
                internal_flux
            } else {
                right_boundary_flux
            },
        }
    }).collect();
    Grid { elements }
}