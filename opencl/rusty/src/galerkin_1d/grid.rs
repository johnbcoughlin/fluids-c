extern crate rulinalg;

use std::fmt;
use functions::jacobi_polynomials::grad_legendre_roots;
use self::rulinalg::vector::Vector;
use galerkin_1d::unknowns::{Unknown};

pub struct Element<U: Unknown> {
    pub index: i32,
    pub x_left: f64,
    pub x_right: f64,

    pub x_k: Vector<f64>,

    pub left_face: Box<Face<U>>,
    pub right_face: Box<Face<U>>,

    pub left_outward_normal: f64,
    pub right_outward_normal: f64,
}

impl<U: Unknown> fmt::Display for Element<U> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "D_{}: [{:.2}, {:.2}]", self.index, self.x_left, self.x_right)
    }
}

pub enum Face<U: Unknown> {
    // An interior face with the index of the element on the other side.
    Interior(i32),

    // A Dirichlet boundary condition which is dependent on time.
    BoundaryDirichlet(Box<Fn(f64) -> U::Unit>),

    // A Neumann boundary condition with specified flux across.
    Neumann(f64),
}

impl<U: Unknown> fmt::Debug for Face<U> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match *self {
            Face::Neumann(_) => write!(f, "||-"),
            Face::BoundaryDirichlet(_) => write!(f, "||="),
            Face::Interior(i) => write!(f, "Interior({})", i),
        }
    }
}

pub struct ReferenceElement {
    // The order of polynomial approximation N_p
    pub n_p: i32,

    // The vector of interpolation points in the reference element [-1, 1].
    // The first value in this vector is -1, and the last is 1.
    pub rs: Vector<f64>,
}

impl ReferenceElement {
    pub fn legendre(n_p: i32) -> ReferenceElement {
        let mut rs = vec![-1.];
        let roots = grad_legendre_roots(n_p);
        for r in roots.into_iter() {
            rs.push(r);
        }
        rs.push(1.);
        let rs = Vector::new(rs);
        ReferenceElement { n_p, rs }
    }
}

pub struct Grid<U: Unknown> {
    pub x_min: f64,
    pub x_max: f64,
    pub elements: Vec<Element<U>>,
}

impl<U: Unknown> fmt::Display for Grid<U> {
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

pub fn generate_grid<U>(x_min: f64, x_max: f64, n_k: i32, n_p: i32,
                              reference_element: &ReferenceElement,
                              left_boundary_face: Face<U>, right_boundary_face: Face<U>) -> Grid<U>
    where U: Unknown {
    assert!(x_max > x_min);
    let diff = (x_max - x_min) / (n_k as f64);
    let transform = |left| {
        let s = (&reference_element.rs + 1.) / 2.;
        let x = s * diff + left;
        x
    };
    let mut elements = vec![];
    elements.push(Element {
        index: 0,
        x_left: x_min,
        x_right: x_min + diff,
        x_k: transform(x_min),
        left_face: Box::new(left_boundary_face),
        right_face: Box::new(Face::Interior(1)),
        left_outward_normal: -1.,
        right_outward_normal: 1.,
    });
    elements.extend((1..n_k - 1).map(|k| {
        let left = x_min + diff * (k as f64);
        Element {
            index: k,
            x_left: left,
            x_right: left + diff,
            x_k: transform(left),
            left_face: Box::new(Face::Interior(k - 1)),
            right_face: Box::new(Face::Interior(k + 1)),
            left_outward_normal: -1.,
            right_outward_normal: 1.,
        }
    }));
    elements.push(Element {
        index: n_k - 1,
        x_left: x_max - diff,
        x_right: x_max,
        x_k: transform(x_max - diff),
        left_face: Box::new(Face::Interior(n_k - 2)),
        right_face: Box::new(right_boundary_face),
        left_outward_normal: -1.,
        right_outward_normal: 1.,
    });
    Grid { x_min, x_max, elements }
}