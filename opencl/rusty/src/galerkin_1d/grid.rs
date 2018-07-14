extern crate arrayfire;

use std::fmt;
use functions::jacobi_polynomials::grad_legendre_roots;
use self::arrayfire::{Array, Dim4};

pub struct Element {
    pub index: i32,
    pub x_left: f32,
    pub x_right: f32,

    pub x_k: Array,

    pub left_face: Box<Face>,
    pub right_face: Box<Face>,

    pub left_outward_normal: f32,
    pub right_outward_normal: f32,
}

impl fmt::Display for Element {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "D_{}: [{:.2}, {:.2}]", self.index, self.x_left, self.x_right)
    }
}

pub enum Face {
    // An interior face with the index of the element on the other side.
    Interior(i32),

    // A Dirichlet boundary condition which is dependent on time.
    BoundaryDirichlet(Box<Fn(f32) -> f32>),

    // A Neumann boundary condition with specified flux across.
    Neumann(f32),
}

impl fmt::Debug for Face {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match *self {
            Face::Neumann(_) => write!(f, "||-"),
            Face::BoundaryDirichlet(_) => write!(f, "||="),
            Face::Interior(i) => write!(f, "|"),
        }
    }
}

pub struct ReferenceElement {
    // The order of polynomial approximation N_p
    pub n_p: i32,

    // The vector of interpolation points in the reference element [-1, 1].
    // The first value in this vector is -1, and the last is 1.
    pub rs: Array,
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
            rs.push(r as f32);
        }
        rs.push(1.);
        let rs = Array::new(rs.as_slice(), Dim4::new(&[rs.len() as u64, 1, 1, 1]));
        ReferenceElement { n_p, rs }
    }
}

pub struct Grid {
    pub elements: Vec<Element>,
}

impl fmt::Display for Grid {
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

pub fn generate_grid(x_min: f32, x_max: f32, n_k: i32, n_p: i32,
                     reference_element: &ReferenceElement,
                     left_boundary_face: Face, right_boundary_face: Face) -> Grid {
    assert!(x_max > x_min);
    let diff = (x_max - x_min) / (n_k as f32);
    let transform = |left| {
        let s = (&reference_element.rs + 1. as f32) / 2. as f32;
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
        let left = x_min + diff * (k as f32);
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
        left_face: Box::new(Face::Interior(n_k - 1)),
        right_face: Box::new(right_boundary_face),
        left_outward_normal: -1.,
        right_outward_normal: 1.,
    });
    Grid { elements }
}