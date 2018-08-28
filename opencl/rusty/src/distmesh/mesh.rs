pub struct Point2D {
    pub x: f64,
    pub y: f64,
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
