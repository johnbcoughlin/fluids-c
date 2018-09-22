extern crate rulinalg;

use galerkin_2d::galerkin::GalerkinScheme;
use galerkin_2d::grid::{ElementStorage, FaceNumber, FaceType, Grid, SpatialVariable};
use galerkin_2d::operators::Operators;
use galerkin_2d::reference_element::ReferenceElement;
use rulinalg::vector::Vector;
use std::cell::Cell;
use std::fmt;
use std::ops::{Add, Div, Mul, Neg};
use galerkin_2d::flux::FluxScheme;

pub trait Unknown {
    type Line: Neg<Output=Self::Line>
    + Add<Output=Self::Line>
    + Mul<f64, Output=Self::Line>
    + Div<f64, Output=Self::Line>
    + fmt::Debug;

    fn zero(reference_element: &ReferenceElement) -> Self;

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

pub fn initialize_storage<GS, Fx>(
    u_0: Fx,
    n_p: i32,
    reference_element: &ReferenceElement,
    grid: &Grid<GS>,
    operators: &Operators,
) -> Vec<ElementStorage<GS::U>>
    where
        GS: GalerkinScheme,
        Fx: Fn(&Vector<f64>) -> GS::U,
{
    let mut result: Vec<ElementStorage<GS::U>> = vec![];
    for (i, elt) in grid.elements.iter().enumerate() {
        let (f_face1_minus, f_face1_plus) = match elt.face1.face_type {
            FaceType::Interior(j, face_number) => (
                elt.spatial_parameters.edge_1(reference_element),
                grid.elements[j as usize].spatial_parameters.face(face_number),
            ),
        };
        let (f_face2_minus, f_face2_plus) = match elt.face2.face_type {
            FaceType::Interior(j, face_number) => (
                elt.spatial_parameters.edge_2(reference_element),
                grid.elements[j as usize].spatial_parameters.face(face_number),
            ),
        };
        let (f_face3_minus, f_face3_plus) = match elt.face3.face_type {
            FaceType::Interior(j, face_number) => (
                elt.spatial_parameters.edge_3(reference_element),
                grid.elements[j as usize].spatial_parameters.face(face_number),
            ),
        };
        result.push(ElementStorage {
            u_k: u_0(&elt.x_k),
            u_face1_minus: Cell::new(GS::U::face1_zero(reference_element)),
            u_face1_plus: Cell::new(GS::U::face1_zero(reference_element)),
            u_face2_minus: Cell::new(GS::U::face2_zero(reference_element)),
            u_face2_plus: Cell::new(GS::U::face2_zero(reference_element)),
            u_face3_minus: Cell::new(GS::U::face3_zero(reference_element)),
            u_face3_plus: Cell::new(GS::U::face3_zero(reference_element)),

            f_face1_minus: Cell::new(f_face1_minus),
            f_face1_plus: Cell::new(f_face1_plus),
            f_face2_minus: Cell::new(f_face2_minus),
            f_face2_plus: Cell::new(f_face2_plus),
            f_face3_minus: Cell::new(f_face3_minus),
            f_face3_plus: Cell::new(f_face3_plus),
        });
    }
    result
}

pub fn communicate<GS>(
    t: f64,
    reference_element: &ReferenceElement,
    grid: &Grid<GS>,
    storages: &Vec<ElementStorage<GS::U>>,
) where
    GS: GalerkinScheme,
{
    for (i, elt) in grid.elements.iter().enumerate() {
        let mut storage: &ElementStorage<GS::U> = storages.get(i).expect("index mismatch");
        let mut u_k: &GS::U = &storage.u_k;

        let face1 = u_k.edge_1(reference_element);
        let (face1_minus, face1_plus) = match elt.face1.face_type {
            FaceType::Interior(j, face_number) => {
                let u_k_neighbor: &GS::U = &storages[j as usize].u_k;
                // minus is interior, plus is neighbor
                (face1, u_k_neighbor.face(face_number, reference_element))
            }
            FaceType::Boundary(bc) => {
                // minus is interior, plus is neighbor
                (face1, bc(t))
            }
        };
        storage.u_face1_minus.set(face1_minus);
        storage.u_face1_plus.set(face1_plus);

        let face2 = u_k.edge_2(reference_element);
        let (face2_minus, face2_plus) = match elt.face2.face_type {
            FaceType::Interior(j, face_number) => {
                let u_k_neighbor: &GS::U = &storages[j as usize].u_k;
                // minus is interior, plus is neighbor
                (face2, u_k_neighbor.face(face_number, reference_element))
            }
            FaceType::Boundary(bc) => {
                // minus is interior, plus is neighbor
                (face2, bc(t))
            }
        };
        storage.u_face2_minus.set(face2_minus);
        storage.u_face2_plus.set(face2_plus);

        let face3 = u_k.edge_3(reference_element);
        let (face3_minus, face3_plus) = match elt.face3.face_type {
            FaceType::Interior(j, face_number) => {
                let u_k_neighbor: &GS::U = &storages[j as usize].u_k;
                // minus is interior, plus is neighbor
                (face3, u_k_neighbor.face(face_number, reference_element))
            }
            FaceType::Boundary(bc) => {
                // minus is interior, plus is neighbor
                (face3, bc(t))
            }
        };
        storage.u_face3_minus.set(face3_minus);
        storage.u_face3_plus.set(face3_plus);
    }
}
