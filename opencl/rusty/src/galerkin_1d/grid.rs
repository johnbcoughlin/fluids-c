extern crate rulinalg;

use self::rulinalg::vector::Vector;
use functions::jacobi_polynomials::grad_legendre_roots;
use galerkin_1d::flux::FluxEnum;
use galerkin_1d::flux::FluxScheme;
use galerkin_1d::galerkin::GalerkinScheme;
use galerkin_1d::unknowns::Unknown;
use std::cell::Cell;
use std::fmt;

pub trait SpatialFlux {
    type Unit: Sized + Copy;

    fn first(&self) -> Self::Unit;

    fn last(&self) -> Self::Unit;

    fn zero() -> Self::Unit;
}

pub struct Element<GS: GalerkinScheme> {
    pub index: i32,
    pub x_left: f64,
    pub x_right: f64,

    pub x_k: Vector<f64>,

    pub left_face: Box<Face<GS>>,
    pub right_face: Box<Face<GS>>,

    pub left_outward_normal: f64,
    pub right_outward_normal: f64,

    // Some representation of the per-element spatial flux.
    pub spatial_flux: GS::F,
}

pub struct ElementStorage<U: Unknown, F: SpatialFlux> {
    // The derivative of r with respect to x, i.e. the metric of the x -> r mapping.
    pub r_x: Vector<f64>,
    pub r_x_at_faces: Vector<f64>,

    pub u_k: U,

    // the interior value on the left face
    pub u_left_minus: Cell<U::Unit>,
    // the exterior value on the left face
    pub u_left_plus: Cell<U::Unit>,
    // the interior value on the right face
    pub u_right_minus: Cell<U::Unit>,
    // the exterior value on the right face
    pub u_right_plus: Cell<U::Unit>,

    pub f_left_minus: Cell<F::Unit>,
    pub f_left_plus: Cell<F::Unit>,
    pub f_right_minus: Cell<F::Unit>,
    pub f_right_plus: Cell<F::Unit>,
}

impl<GS: GalerkinScheme> fmt::Display for Element<GS> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "D_{}: [{:.2}, {:.2}]",
            self.index, self.x_left, self.x_right
        )
    }
}

impl<U: Unknown, F: SpatialFlux> fmt::Debug for ElementStorage<U, F> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{{\n")?;
        write!(f, "\tu_left_minus: {:?},\n", self.u_left_minus)?;
        write!(f, "\tu_left_plus: {:?},\n", self.u_left_plus)?;
        write!(f, "\tu_right_minus: {:?},\n", self.u_right_minus)?;
        write!(f, "\tu_right_plus: {:?},\n", self.u_right_plus)?;
        write!(f, "}}")
    }
}

pub enum FaceType<GS: GalerkinScheme> {
    // An interior face with the index of the element on the other side.
    Interior(i32),

    // A complex boundary condition which may depend on both the other side of the boundary
    // and the time parameter
    Boundary(
        Box<
            Fn(f64, <<GS as GalerkinScheme>::U as Unknown>::Unit)
                -> <<GS as GalerkinScheme>::U as Unknown>::Unit,
        >,
        <<GS as GalerkinScheme>::F as SpatialFlux>::Unit,
    ),
}

pub struct Face<GS: GalerkinScheme> {
    pub face_type: FaceType<GS>,
    pub flux: FluxEnum<GS::U, GS::F, GS::FS>,
}

pub fn freeFlowBoundary<GS: GalerkinScheme>(
    f: <<GS as GalerkinScheme>::F as SpatialFlux>::Unit,
) -> FaceType<GS> {
    FaceType::Boundary(Box::new(move |_, other_side| other_side), f)
}

impl<GS: GalerkinScheme> fmt::Debug for FaceType<GS> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match *self {
            FaceType::Boundary(_, _) => write!(f, "||="),
            FaceType::Interior(i) => write!(f, "Interior({})", i),
        }
    }
}

pub struct ReferenceElement {
    // The order of polynomial approximation N_p
    pub n_p: i32,

    // The vector of interpolation points in the reference element [-1, 1].
    // The first value in this vector is -1, and the last is 1.
    pub rs: Vector<f64>,
}

impl ReferenceElement {
    pub fn legendre(n_p: i32) -> ReferenceElement {
        let mut rs = vec![-1.];
        let roots = grad_legendre_roots(n_p);
        for r in roots.into_iter() {
            rs.push(r);
        }
        rs.push(1.);
        let rs = Vector::new(rs);
        ReferenceElement { n_p, rs }
    }
}

pub struct Grid<GS: GalerkinScheme> {
    pub x_min: f64,
    pub x_max: f64,
    pub elements: Vec<Element<GS>>,
}

impl<GS: GalerkinScheme> fmt::Display for Grid<GS> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        let elts = &self.elements;
        write!(f, "[ ")?;
        for (count, elt) in elts.iter().enumerate() {
            if count != 0 {
                write!(f, ", ")?;
            }
            write!(f, "{}\n", elt)?;
        }
        write!(f, "]")
    }
}

pub fn generate_grid<GS, Fx>(
    x_min: f64,
    x_max: f64,
    n_k: i32,
    reference_element: &ReferenceElement,
    left_boundary: Face<GS>,
    right_boundary: Face<GS>,
    interior_flux: <GS::FS as FluxScheme<GS::U, GS::F>>::Interior,
    f: Fx,
) -> Grid<GS>
where
    GS: GalerkinScheme,
    Fx: Fn(&Vector<f64>) -> GS::F,
{
    assert!(x_max > x_min);
    let diff = (x_max - x_min) / (n_k as f64);
    let transform = |left| {
        let s = (&reference_element.rs + 1.) / 2.;
        let x = s * diff + left;
        x
    };
    let mut elements = vec![];
    let x_k = transform(x_min);
    let spatial_flux = f(&x_k);
    elements.push(Element {
        index: 0,
        x_left: x_min,
        x_right: x_min + diff,
        x_k,
        left_face: Box::new(left_boundary),
        right_face: Box::new(Face {
            face_type: FaceType::Interior(1),
            flux: FluxEnum::Interior(interior_flux),
        }),
        left_outward_normal: -1.,
        right_outward_normal: 1.,
        spatial_flux,
    });
    elements.extend((1..n_k - 1).map(|k| {
        let left = x_min + diff * (k as f64);
        let x_k = transform(left);
        let spatial_flux = f(&x_k);
        Element {
            index: k,
            x_left: left,
            x_right: left + diff,
            x_k,
            left_face: Box::new(Face {
                face_type: FaceType::Interior(k - 1),
                flux: FluxEnum::Interior(interior_flux),
            }),
            right_face: Box::new(Face {
                face_type: FaceType::Interior(k + 1),
                flux: FluxEnum::Interior(interior_flux),
            }),
            left_outward_normal: -1.,
            right_outward_normal: 1.,
            spatial_flux,
        }
    }));
    let x_k = transform(x_max - diff);
    let spatial_flux = f(&x_k);
    elements.push(Element {
        index: n_k - 1,
        x_left: x_max - diff,
        x_right: x_max,
        x_k,
        left_face: Box::new(Face {
            face_type: FaceType::Interior(n_k - 2),
            flux: FluxEnum::Interior(interior_flux),
        }),
        right_face: Box::new(right_boundary),
        left_outward_normal: -1.,
        right_outward_normal: 1.,
        spatial_flux,
    });
    Grid {
        x_min,
        x_max,
        elements,
    }
}
