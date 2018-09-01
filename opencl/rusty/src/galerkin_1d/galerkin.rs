extern crate rulinalg;
extern crate typenum;
extern crate generic_array;

use self::generic_array::ArrayLength;
use galerkin_1d::unknowns::Unknown;
use galerkin_1d::grid::SpatialFlux;
use galerkin_1d::flux::FluxScheme;
use galerkin_1d::flux::NumericalFlux;
use galerkin_1d::grid::Element;
use galerkin_1d::grid::ElementStorage;
use galerkin_1d::flux::FluxEnum;
use galerkin_1d::flux::Side;
use self::typenum::{Unsigned, Add1, Sub1};
use std::ops::{Add, Sub};
use galerkin_1d::grid::ReferenceElement;

pub trait GalerkinScheme {
    type U: Unknown;
    type F: SpatialFlux;
    type FS: FluxScheme<Self::U, Self::F>;
    type NP: Unsigned + ArrayLength<f64>;
    type RE: ReferenceElement<NP=Self::NP>;

    const FORMULATION: Formulation;
}

pub enum Formulation {
    Strong,
    Weak,
}

pub fn compute_flux<GS: GalerkinScheme>(
    elt: &Element<GS>,
    elt_storage: &ElementStorage<GS::U, GS::F>,
) -> (<GS::U as Unknown>::Unit, <GS::U as Unknown>::Unit) {
    let left_du = {
        let minus = Side { u: elt_storage.u_left_minus.get(), f: elt_storage.f_left_minus.get() };
        let plus = Side { u: elt_storage.u_left_plus.get(), f: elt_storage.f_left_plus.get() };
        match elt.left_face.flux {
            FluxEnum::Left(ref f) => f.flux(minus, plus, elt.left_outward_normal),
            FluxEnum::Interior(ref f) => f.flux(minus, plus, elt.left_outward_normal),
            FluxEnum::Right(_) => panic!("Right side flux found on left face"),
        }
    };

    let right_du = {
        let minus = Side { u: elt_storage.u_right_minus.get(), f: elt_storage.f_right_minus.get() };
        let plus = Side { u: elt_storage.u_right_plus.get(), f: elt_storage.f_right_plus.get() };
        match elt.right_face.flux {
            FluxEnum::Left(_) => panic!("Left side flux found on right face"),
            FluxEnum::Interior(ref f) => f.flux(minus, plus, elt.right_outward_normal),
            FluxEnum::Right(ref f) => f.flux(minus, plus, elt.right_outward_normal),
        }
    };

    (left_du, right_du)
}
