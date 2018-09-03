use galerkin_2d::operators::{assemble_operators, Operators};
use galerkin_2d::grid::{ReferenceElement, };

pub fn maxwell_2d() {
    let n_p = 10;
    let reference_element = ReferenceElement::legendre(n_p);
    let operators = assemble_operators(&reference_element);
}

#[cfg(test)]
mod tests {
    use super::{maxwell_2d};

    #[test]
    pub fn test_maxwell_2d() {
        maxwell_2d();
    }
}