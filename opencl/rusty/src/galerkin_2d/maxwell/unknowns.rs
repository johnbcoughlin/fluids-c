extern crate rulinalg;

use galerkin_2d::operators::{assemble_operators, Operators};
use std::ops::{Neg, Add, Mul, Div};
use galerkin_2d::reference_element::{ReferenceElement, };
use galerkin_2d::grid::{Grid, ElementStorage, assemble_grid, };
use distmesh::distmesh_2d::ellipse;
use galerkin_2d::galerkin::GalerkinScheme;
use galerkin_2d::unknowns::{Unknown};
use rulinalg::vector::Vector;

pub type EHStorage = ElementStorage<EH>;

#[derive(Debug, Clone, Copy)]
pub struct EHUnit {
    pub E: f64,
    pub H: f64,
}

#[derive(Debug)]
pub struct EH {
    pub E: Vector<f64>,
    pub H: Vector<f64>,
}

impl Unknown for EH {
    type Unit = EHUnit;
    type Line = EH;

    fn edge_1(&self, reference_element: &ReferenceElement) -> EH {
        EH {
            E: self.E.select(reference_element.face1.as_slice()),
            H: self.H.select(reference_element.face1.as_slice()),
        }
    }

    fn edge_2(&self, reference_element: &ReferenceElement) -> Self::Line {
        EH {
            E: self.E.select(reference_element.face2.as_slice()),
            H: self.H.select(reference_element.face2.as_slice()),
        }
    }

    fn edge_3(&self, reference_element: &ReferenceElement) -> Self::Line {
        EH {
            E: self.E.select(reference_element.face3.as_slice()),
            H: self.H.select(reference_element.face3.as_slice()),
        }
    }

    fn zero() -> Self::Unit {
        EHUnit {
            E: 0.,
            H: 0.,
        }
    }

    fn face1_zero(reference_element: &ReferenceElement) -> Self::Line {
        EH {
            E: Vector::zeros(reference_element.face1.len()),
            H: Vector::zeros(reference_element.face1.len()),
        }
    }

    fn face2_zero(reference_element: &ReferenceElement) -> Self::Line {
        EH {
            E: Vector::zeros(reference_element.face2.len()),
            H: Vector::zeros(reference_element.face2.len()),
        }
    }

    fn face3_zero(reference_element: &ReferenceElement) -> Self::Line {
        EH {
            E: Vector::zeros(reference_element.face3.len()),
            H: Vector::zeros(reference_element.face3.len()),
        }
    }
}

impl Neg for EHUnit {
    type Output = Self;

    fn neg(self: EHUnit) -> EHUnit {
        EHUnit {
            E: -self.E,
            H: -self.H,
        }
    }
}

impl Add for EHUnit {
    type Output = Self;

    fn add(self, rhs: EHUnit) -> EHUnit {
        EHUnit {
            E: self.E + rhs.E,
            H: self.H + rhs.H,
        }
    }
}

impl Mul<f64> for EHUnit {
    type Output = EHUnit;

    fn mul(self, rhs: f64) -> Self {
        EHUnit {
            E: self.E * rhs,
            H: self.H * rhs,
        }
    }
}

impl Div<f64> for EHUnit {
    type Output = EHUnit;

    fn div(self, rhs: f64) -> Self {
        EHUnit {
            E: self.E / rhs,
            H: self.H / rhs,
        }
    }
}

impl Neg for EH {
    type Output = Self;

    fn neg(self: EH) -> EH {
        EH {
            E: -self.E,
            H: -self.H,
        }
    }
}

impl Add for EH {
    type Output = Self;

    fn add(self, rhs: EH) -> EH {
        EH {
            E: self.E + rhs.E,
            H: self.H + rhs.H,
        }
    }
}

impl Mul<f64> for EH {
    type Output = EH;

    fn mul(self, rhs: f64) -> Self {
        EH {
            E: self.E * rhs,
            H: self.H * rhs,
        }
    }
}

impl Div<f64> for EH {
    type Output = EH;

    fn div(self, rhs: f64) -> Self {
        EH {
            E: self.E / rhs,
            H: self.H / rhs,
        }
    }
}
