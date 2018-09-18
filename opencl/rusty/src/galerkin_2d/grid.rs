extern crate rulinalg;

use distmesh::mesh::{Mesh, Triangle};
use galerkin_2d::galerkin::GalerkinScheme;
use galerkin_2d::operators::Operators;
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::unknowns::Unknown;
use rulinalg::vector::Vector;
use std::cell::Cell;
use std::collections::HashMap;

#[derive(Clone, Copy)]
pub enum FaceNumber {
    One,
    Two,
    Three,
}

pub enum FaceType<'grid, GS: GalerkinScheme>
where
    <GS::U as Unknown>::Line: 'grid,
{
    // An interior face with the index of the element on the other side.
    Interior(i32, FaceNumber),

    // A complex boundary condition which may depend on the other side of the boundary and on
    // the time parameter.
    Boundary(&'grid Fn(f64) -> <GS::U as Unknown>::Line),
}

pub struct Face<'grid, GS: GalerkinScheme>
where
    <GS::U as Unknown>::Line: 'grid,
{
    pub face_type: FaceType<'grid, GS>,
}

pub struct Element<'grid, GS: GalerkinScheme>
where
    <GS::U as Unknown>::Line: 'grid,
{
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

    pub face1: Face<'grid, GS>,
    pub face2: Face<'grid, GS>,
    pub face3: Face<'grid, GS>,
}

pub struct ElementStorage<U: Unknown> {
    pub u_k: U,

    // minus is interior, plus is exterior
    pub u_face1_minus: Cell<U::Line>,
    pub u_face1_plus: Cell<U::Line>,
    pub u_face2_minus: Cell<U::Line>,
    pub u_face2_plus: Cell<U::Line>,
    pub u_face3_minus: Cell<U::Line>,
    pub u_face3_plus: Cell<U::Line>,
}

pub struct Grid<'grid, GS: GalerkinScheme>
where
    <GS::U as Unknown>::Line: 'grid,
{
    pub elements: Vec<Element<'grid, GS>>,
}

pub fn assemble_grid<'grid, GS, F>(
    reference_element: &ReferenceElement,
    operators: &Operators,
    mesh: &Mesh,
    boundary_condition: &'grid F,
) -> Grid<'grid, GS>
where
    GS: GalerkinScheme,
    F: Fn(f64) -> <GS::U as Unknown>::Line + 'grid,
{
    let points = &mesh.points;
    let rs = &reference_element.rs;
    let ss = &reference_element.ss;
    let triangles = &mesh.triangles;

    let mut edges_to_triangle: HashMap<Edge, EdgeType> = HashMap::new();
    for (i, ref triangle) in mesh.triangles.iter().enumerate() {
        let (e1, e2, e3) = triangle.edges();
        let modifier = |e: Edge, face_number: FaceNumber, map: &mut HashMap<Edge, EdgeType>| {
            let new_value = if map.contains_key(&e) {
                let existing = map.get(&e).expect("we just checked");
                existing.with_other_triangle(i as i32, face_number)
            } else {
                EdgeType::Exterior(i as i32, face_number)
            };
            map.insert(e, new_value);
        };
        modifier(e1, FaceNumber::One, &mut edges_to_triangle);
        modifier(e2, FaceNumber::Two, &mut edges_to_triangle);
        modifier(e3, FaceNumber::Three, &mut edges_to_triangle);
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

        let (e1, e2, e3) = triangle.edges();
        let edge_to_face_type = |e: &Edge| match edges_to_triangle.get(e) {
            Some(EdgeType::Interior(a, a_number, b, b_number)) => if *a == i as i32 {
                FaceType::Interior(*b, *b_number)
            } else {
                FaceType::Interior(*a, *a_number)
            },
            Some(EdgeType::Exterior(_, _)) => FaceType::Boundary(boundary_condition),
            None => panic!("edge_to_triangle did not contain {:?}", e),
        };
        let face1: Face<'grid, GS> = Face {
            face_type: edge_to_face_type(&e1),
        };
        let face2: Face<'grid, GS> = Face {
            face_type: edge_to_face_type(&e2),
        };
        let face3: Face<'grid, GS> = Face {
            face_type: edge_to_face_type(&e3),
        };

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
            face1,
            face2,
            face3,
        });
    }

    Grid { elements }
}

#[derive(Debug, PartialEq, Eq, Hash)]
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
    fn edges(&self) -> (Edge, Edge, Edge) {
        (
            Edge::from(self.a, self.b),
            Edge::from(self.b, self.c),
            Edge::from(self.c, self.a),
        )
    }
}

#[derive(Clone, Copy)]
enum EdgeType {
    Exterior(i32, FaceNumber),
    Interior(i32, FaceNumber, i32, FaceNumber),
}

impl EdgeType {
    fn with_other_triangle(&self, triangle: i32, neighbors_face_number: FaceNumber) -> EdgeType {
        match self {
            EdgeType::Exterior(t1, t1_number) => {
                EdgeType::Interior(*t1, *t1_number, triangle, neighbors_face_number)
            }
            EdgeType::Interior(_, _, _, _) => panic!("found an edge with more than two faces"),
        }
    }
}

pub struct Vec2 {
    x: f64,
    y: f64,
}

pub trait SpatialVariable {
    type Line;

    fn edge_1(&self, reference_element: &ReferenceElement) -> Self::Line;

    fn edge_2(&self, reference_element: &ReferenceElement) -> Self::Line;

    fn edge_3(&self, reference_element: &ReferenceElement) -> Self::Line;

    fn face(&self, number: FaceNumber, reference_element: &ReferenceElement) -> Self::Line {
        match number {
            FaceNumber::One => self.edge_1(reference_element),
            FaceNumber::Two => self.edge_2(reference_element),
            FaceNumber::Three => self.edge_3(reference_element),
        }
    }

    fn face1_zero(reference_element: &ReferenceElement) -> Self::Line;

    fn face2_zero(reference_element: &ReferenceElement) -> Self::Line;

    fn face3_zero(reference_element: &ReferenceElement) -> Self::Line;
}
