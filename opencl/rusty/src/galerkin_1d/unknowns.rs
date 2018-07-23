extern crate rulinalg;

use std::fmt;
use std::cell::{Cell, RefCell};
use self::rulinalg::vector::Vector;

pub trait Unknown {
    type Unit: Copy + fmt::Debug;

    fn first(&self) -> Self::Unit;

    fn last(&self) -> Self::Unit;
}

pub struct ElementStorage<U: Unknown> {

    // The derivative of r with respect to x, i.e. the metric of the x -> r mapping.
    pub r_x: Vector<f64>,
    pub r_x_at_faces: Vector<f64>,

    pub u_k: RefCell<U>,
    // the interior value on the left face
    pub u_left_minus: Cell<Option<U::Unit>>,
    // the exterior value on the left face
    pub u_left_plus: Cell<Option<U::Unit>>,
    // the interior value on the right face
    pub u_right_minus: Cell<Option<U::Unit>>,
    // the exterior value on the right face
    pub u_right_plus: Cell<Option<U::Unit>>,
}

impl<U: Unknown> fmt::Debug for ElementStorage<U> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{{\n")?;
        write!(f, "\tu_left_minus: {:?},\n", self.u_left_minus)?;
        write!(f, "\tu_left_plus: {:?},\n", self.u_left_plus)?;
        write!(f, "\tu_right_minus: {:?},\n", self.u_right_minus)?;
        write!(f, "\tu_right_plus: {:?},\n", self.u_right_minus)?;
        write!(f, "}}")
    }
}

