use galerkin_2d::operators::{assemble_operators, Operators};
use galerkin_2d::reference_element::{ReferenceElement, };
use galerkin_2d::grid::{Grid, assemble_grid, };
use distmesh::distmesh_2d::ellipse;

pub fn maxwell_2d() {
    let n_p = 10;
    let reference_element = ReferenceElement::legendre(n_p);
    let operators = assemble_operators(&reference_element);
    let mesh = ellipse();
    let grid = assemble_grid(&reference_element, &operators, &mesh);
}

#[cfg(test)]
mod tests {
    use super::{maxwell_2d};

    #[test]
    pub fn test_maxwell_2d() {
        maxwell_2d();
    }
}