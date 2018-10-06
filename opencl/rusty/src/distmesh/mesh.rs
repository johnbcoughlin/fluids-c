use std::fmt;

pub struct Point2D {
    pub x: f64,
    pub y: f64,
}

impl fmt::Display for Point2D {
    fn fmt(&self, f: &mut fmt::Formatter) -> Result<(), fmt::Error> {
        write!(f, "({}, {})", self.x, self.y)
    }
}

pub struct Triangle {
    // Indices referring to the points of the mesh
    pub a: i32,
    pub b: i32,
    pub c: i32,
}

pub struct Mesh {
    pub points: Vec<Point2D>,
    pub triangles: Vec<Triangle>,
}
