
use galerkin_1d::grid::{Element, ReferenceElement, Grid, Flux};
use std::num;
use std::cell::Cell;

pub fn advec_1d(grid: &Grid, reference_element: &ReferenceElement) {
    let nt = 100;

    let mut storage: Vec<ElementStorage> = initialize_storage(grid);
    for t in 1..nt {
        for int_rk in 1..5 {
            communicate(t as f64, grid, &storage);
            for elt in (*grid).elements.iter() {
            }
        }
    }
}

fn advec_rhs_1d() {

}

fn initialize_storage(grid: &Grid) -> Vec<ElementStorage> {
    grid.elements.iter().map(|elt| {
        ElementStorage {
            u_k: elt.u_k.clone(),
            u_left_minus: Cell::new(None),
            u_right_minus: Cell::new(None),
            u_left_plus: Cell::new(None),
            u_right_plus: Cell::new(None),
        }
    }).collect()
}

// Pass flux information across faces into each element's local storage.
fn communicate(t: f64, grid: &Grid, storages: &Vec<ElementStorage>) {
    for (i, elt) in grid.elements.iter().enumerate() {
        let storage = storages.get(i).expect("index mismatch");
        let (minus, plus) = match *elt.left_flux {
            Flux::Zero => (None, None),
            Flux::BoundaryTimeDependent(_, ref u_0) => {
                let u_0 = u_0(t);
                (Some(u_0), Some(u_0))
            }
            Flux::LaxFriedrichs(_) => (
                Some(*storages[i - 1].u_k.last().expect("vector u_k must not be empty")),
                Some(*storage.u_k.first().expect("vector u_k must not be empty"))
            ),
        };
        storage.u_left_minus.set(minus); storage.u_left_plus.set(plus);

        let (minus, plus) = match *elt.right_flux {
            Flux::Zero => (None, None),
            Flux::BoundaryTimeDependent(_, ref u_0) => {
                let u_0 = u_0(t);
                (Some(u_0), Some(u_0))
            }
            Flux::LaxFriedrichs(_) => (
                Some(*storage.u_k.last().expect("vector u_k must not be empty")),
                Some(*storages[i + 1].u_k.first().expect("vector u_k must not be empty"))
            ),
        };
        storage.u_right_minus.set(minus); storage.u_right_plus.set(plus);
    }
}

struct ElementStorage {
    u_k: Vec<f64>,
    u_left_minus: Cell<Option<f64>>,
    u_left_plus: Cell<Option<f64>>,
    u_right_minus: Cell<Option<f64>>,
    u_right_plus: Cell<Option<f64>>,
}

#[cfg(test)]
mod tests {
    use galerkin_1d::grid::{Element, ReferenceElement, Grid, Flux, generate_grid,};
    use galerkin_1d::advec::advec_1d;

    #[test]
    fn test() {
        let n_p = 8;
        let reference_element = ReferenceElement::legendre(n_p);
        let u_0 = |x: f64| x.sin();
        let left_boundary_flux = Flux::BoundaryTimeDependent(1.0, Box::new(|t: f64| t.sin()));
        let right_boundary_flux = Flux::Zero;
        let internal_flux = Flux::LaxFriedrichs(1.0);
        let grid: Grid = generate_grid(0.0, 4.0, 10, 3, &reference_element, u_0,
                                       &left_boundary_flux, &right_boundary_flux, &internal_flux);

        advec_1d(&grid, &reference_element);
    }
}