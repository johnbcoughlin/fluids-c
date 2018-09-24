extern crate rulinalg;

use distmesh::distmesh_2d::ellipse;
use galerkin_2d::galerkin::GalerkinScheme;
use galerkin_2d::grid::{assemble_grid, ElementStorage, Grid};
use galerkin_2d::operators::FaceLift;
use galerkin_2d::operators::FaceLiftable;
use galerkin_2d::operators::{assemble_operators, Operators};
use galerkin_2d::reference_element::ReferenceElement;
use galerkin_2d::unknowns::Unknown;
use rulinalg::vector::Vector;
use std::ops::{Add, Div, Mul, Neg, Sub};

#[derive(Debug, Clone, Copy)]
pub struct EHUnit {
    pub Ez: f64,
    pub Hx: f64,
    pub Hy: f64,
}

#[derive(Debug)]
pub struct EH {
    pub Ez: Vector<f64>,
    pub Hx: Vector<f64>,
    pub Hy: Vector<f64>,
}

impl FaceLiftable for EH {
    fn lift_faces(
        face_lift: &FaceLift,
        face1: &<Self as Unknown>::Line,
        face2: &<Self as Unknown>::Line,
        face3: &<Self as Unknown>::Line,
    ) -> Self {
        let face1_lifted = EH {
            Ez: face_lift.face1 * face1.Ez,
            Hx: face_lift.face1 * face1.Hx,
            Hy: face_lift.face1 * face1.Hy,
        };
        let face2_lifted = EH {
            Ez: face_lift.face2 * face2.Ez,
            Hx: face_lift.face2 * face2.Hx,
            Hy: face_lift.face2 * face2.Hy,
        };
        let face3_lifted = EH {
            Ez: face_lift.face3 * face3.Ez,
            Hx: face_lift.face3 * face3.Hx,
            Hy: face_lift.face3 * face3.Hy,
        };
        face1_lifted + face2_lifted + face3_lifted
    }
}

impl Unknown for EH {
    type Line = EH;

    fn edge_1(&self, reference_element: &ReferenceElement) -> EH {
        EH {
            Ez: self.Ez.select(reference_element.face1.as_slice()),
            Hx: self.Hx.select(reference_element.face1.as_slice()),
            Hy: self.Hy.select(reference_element.face1.as_slice()),
        }
    }

    fn edge_2(&self, reference_element: &ReferenceElement) -> Self::Line {
        EH {
            Ez: self.Ez.select(reference_element.face2.as_slice()),
            Hx: self.Hx.select(reference_element.face2.as_slice()),
            Hy: self.Hy.select(reference_element.face2.as_slice()),
        }
    }

    fn edge_3(&self, reference_element: &ReferenceElement) -> Self::Line {
        EH {
            Ez: self.Ez.select(reference_element.face3.as_slice()),
            Hx: self.Hx.select(reference_element.face3.as_slice()),
            Hy: self.Hy.select(reference_element.face3.as_slice()),
        }
    }

    fn zero(reference_element: &ReferenceElement) -> Self {
        EH {
            Ez: Vector::zeros(reference_element.n_p),
            Hx: Vector::zeros(reference_element.n_p),
            Hy: Vector::zeros(reference_element.n_p),
        }
    }

    fn face1_zero(reference_element: &ReferenceElement) -> Self::Line {
        EH {
            Ez: Vector::zeros(reference_element.face1.len()),
            Hx: Vector::zeros(reference_element.face1.len()),
            Hy: Vector::zeros(reference_element.face1.len()),
        }
    }

    fn face2_zero(reference_element: &ReferenceElement) -> Self::Line {
        EH {
            Ez: Vector::zeros(reference_element.face2.len()),
            Hx: Vector::zeros(reference_element.face2.len()),
            Hy: Vector::zeros(reference_element.face2.len()),
        }
    }

    fn face3_zero(reference_element: &ReferenceElement) -> Self::Line {
        EH {
            Ez: Vector::zeros(reference_element.face3.len()),
            Hx: Vector::zeros(reference_element.face3.len()),
            Hy: Vector::zeros(reference_element.face3.len()),
        }
    }
}

impl Neg for EH {
    type Output = Self;

    fn neg(self: EH) -> EH {
        EH {
            Ez: -self.Ez,
            Hx: -self.Hx,
            Hy: -self.Hy,
        }
    }
}

impl Add for EH {
    type Output = Self;

    fn add(self, rhs: EH) -> EH {
        EH {
            Ez: self.Ez + rhs.Ez,
            Hx: self.Hx + rhs.Hx,
            Hy: self.Hy + rhs.Hy,
        }
    }
}

impl Sub for EH {
    type Output = Self;

    fn sub(self, rhs: EH) -> EH {
        EH {
            Ez: self.Ez - rhs.Ez,
            Hx: self.Hx - rhs.Hx,
            Hy: self.Hy - rhs.Hy,
        }
    }
}

impl Mul<f64> for EH {
    type Output = EH;

    fn mul(self, rhs: f64) -> Self {
        EH {
            Ez: self.Ez * rhs,
            Hx: self.Hx * rhs,
            Hy: self.Hy * rhs,
        }
    }
}

impl Div<f64> for EH {
    type Output = EH;

    fn div(self, rhs: f64) -> Self {
        EH {
            Ez: self.Ez / rhs,
            Hx: self.Hx / rhs,
            Hy: self.Hy / rhs,
        }
    }
}
