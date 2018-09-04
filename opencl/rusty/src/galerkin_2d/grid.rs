extern crate rulinalg;

use rulinalg::vector::Vector;
use distmesh::mesh::{Mesh, Triangle};
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::operators::Operators;
use std::collections::HashMap;

pub struct Element {
    pub index: i32,
    pub x_k: Vector<f64>,
    pub y_k: Vector<f64>,

    // Derivatives of the metric mapping at each point
    // dx/dr
    pub x_r: Vector<f64>,
    // dy/dr
    pub y_r: Vector<f64>,
    // dx/ds
    pub x_s: Vector<f64>,
    // dy/ds
    pub y_s: Vector<f64>,
    // The Jacobian, x_r * y_s - x_s * y_r
    pub jacobian: Vector<f64>,

    // derivatives in the other direction
    r_x: Vector<f64>,
    s_x: Vector<f64>,
    r_y: Vector<f64>,
    s_y: Vector<f64>,
}

pub struct Grid {
    elements: Vec<Element>,
}

pub fn assemble_grid(
    reference_element: &ReferenceElement,
    operators: &Operators,
    mesh: &Mesh) -> Grid {
    let points = &mesh.points;
    let rs = &reference_element.rs;
    let ss = &reference_element.ss;
    let triangles = &reference_element.triangles;

    let mut edges_to_triangle: HashMap<Edge, EdgeType> = HashMap::new();
    for (i, ref triangle) in mesh.triangles.iter().enumerate() {
        let (e1, e2, e3) = triangle.edges();
        let edge_creator = || EdgeType::Exterior(i as usize);
        edges_to_triangle.entry(e1).or_insert(edge_creator);
        edges_to_triangle.entry(e2).or_insert(edge_creator);
        edges_to_triangle.entry(e3).or_insert(edge_creator);
    }

    let mut elements = Vec::new();

    for (i, ref triangle) in mesh.triangles.iter().enumerate() {
        let (ref a, ref b, ref c) = (
            &points[triangle.a as usize],
            &points[triangle.b as usize],
            &points[triangle.c as usize],
        );

        let x: Vector<f64> = -(&(rs + ss) * a.x + (rs + 1.) * b.x + (ss + 1.) * c.x) * 0.5;
        let y: Vector<f64> = -(&(rs + ss) * a.y + (rs + 1.) * b.y + (ss + 1.) * c.x) * 0.5;

        let x_r = &operators.d_r * &x;
        let x_s = &operators.d_s * &x;
        let y_r = &operators.d_r * &y;
        let y_s = &operators.d_s * &y;
        let jacobian = x_r.elemul(&y_s) - &(x_s.elemul(&y_r));

        let r_x = y_s.elediv(&jacobian);
        let s_x = -y_r.elediv(&jacobian);
        let r_y = x_s.elediv(&jacobian);
        let s_y = -x_r.elediv(&jacobian);

        elements.push(Element {
            index: i as i32,
            x_k: x,
            y_k: y,
            x_r,
            y_r,
            x_s,
            y_s,
            jacobian,
            r_x,
            s_x,
            r_y,
            s_y,
        });
    }

    Grid { elements }
}

struct Edge {
    n1: i32,
    n2: i32,
}

impl Edge {
    fn from(a: i32, b: i32) -> Edge {
        if a > b {
            Edge { n1: b, n2: a }
        } else {
            Edge { n1: a, n2: b }
        }
    }
}

impl Triangle {
    fn edges(&self) -> (Edge, Edge, Edge, ) {
        (
            Edge::from(self.a, self.b),
            Edge::from(self.b, self.c),
            Edge::from(self.c, self.a),
        )
    }
}

enum EdgeType {
    None,
    Exterior(i32),
    Interior(i32, i32),
}

impl EdgeType {
    fn with_other_triangle(self, triangle: i32) -> EdgeType {
        match self {
            EdgeType::None => EdgeType::Exterior(triangle),
            EdgeType::Exterior(t1) => EdgeType::Interior(t1, triangle),
            EdgeType::Interior(_, _) => panic!("found an edge with more than two faces"),
        }
    }
}
