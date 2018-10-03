use distmesh::mesh::Mesh;
use distmesh::mesh::Point2D;
use distmesh::mesh::Triangle;
use std::num::ParseFloatError;
use std::num::ParseIntError;
use std::str::FromStr;

pub fn ellipse() -> Mesh {
    parse_distmesh_2d(
        include_str!("../../static/meshes_2d/ellipse_points"),
        include_str!("../../static/meshes_2d/ellipse_triangles"),
    )
}

pub fn unit_square() -> Mesh {
    parse_distmesh_2d(
        include_str!("../../static/meshes_2d/unit_square_points"),
        include_str!("../../static/meshes_2d/unit_square_triangles"),
    )
}

pub fn parse_distmesh_2d(points_file: &str, triangles_file: &str) -> Mesh {
    let points = points_file
        .split("\n")
        .map(|line: &str| line.parse::<Point2D>().expect("error parsing point: "))
        .collect();
    let triangles = triangles_file
        .split("\n")
        .map(|line: &str| line.parse::<Triangle>().expect("error parsing triangle: "))
        .collect();

    Mesh { points, triangles }
}

impl FromStr for Point2D {
    type Err = ParseFloatError;

    // from a tab-separated pair of floats
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let coords: Vec<&str> = s.split("\t").collect();
        let x = coords[0].parse::<f64>()?;
        let y = coords[1].parse::<f64>()?;
        Ok(Point2D { x, y })
    }
}

impl FromStr for Triangle {
    type Err = ParseIntError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let coords: Vec<&str> = s.split("\t").collect();
        // subtract 1 because Matlab is 1-indexed
        let a = coords[0].parse::<i32>()? - 1;
        let b = coords[1].parse::<i32>()? - 1;
        let c = coords[2].parse::<i32>()? - 1;
        Ok(Triangle { a, b, c })
    }
}
