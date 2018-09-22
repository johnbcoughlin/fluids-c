use galerkin_2d::grid::SpatialVariable;
use galerkin_2d::grid::Vec2;
use galerkin_2d::unknowns::Unknown;
use galerkin_2d::galerkin::GalerkinScheme;
use galerkin_2d::grid::Element;
use galerkin_2d::grid::ElementStorage;

pub struct Side<U, F>
    where
        U: Unknown,
        F: SpatialVariable,
{
    pub u: <U as Unknown>::Line,
    pub f: F::Line,
}

pub trait FluxKey {}

pub trait FluxScheme<U>
    where
        U: Unknown,
{
    type F: SpatialVariable;
    type K: FluxKey;

    fn flux_type(
        key: Self::K,
        minus: Side<U, Self::F>,
        plus: Side<U, Self::F>,
        outward_normal: Vec2,
    ) -> U::Line;
}

pub trait NumericalFlux<U, F>
    where
        U: Unknown,
        F: SpatialVariable,
{
    fn flux(&self, minus: Side<U, F>, plus: Side<U, F>, outward_normal: Vec2) -> U::Line;
}

pub fn compute_flux<'grid, GS>(
    elt: &Element<'grid, GS>,
    elt_storage: &ElementStorage<GS::U>,
) -> (<GS::U as Unknown>::Line, <GS::U as Unknown>::Line, <GS::U as Unknown>::Line,)
    where
        GS: GalerkinScheme {
    let face1_flux = {
        let minus = Side {
            u: elt_storage.u_face1_minus.get(),
            f: elt_storage.f_face1_minus.get(),
        };
        let plus = Side {
            u: elt_storage.u_face1_plus.get(),
            f: elt_storage.f_face1_plus.get(),
        };
        <GS::FS as FluxScheme>::flux_type(elt.face1.flux_key, minus, plus, elt.face1.outward_normal)
    };
    let face2_flux = {
        let minus = Side {
            u: elt_storage.u_face2_minus.get(),
            f: elt_storage.f_face2_minus.get(),
        };
        let plus = Side {
            u: elt_storage.u_face2_plus.get(),
            f: elt_storage.f_face2_plus.get(),
        };
        <GS::FS as FluxScheme>::flux_type(elt.face2.flux_key, minus, plus, elt.face2.outward_normal)
    };
    let face3_flux = {
        let minus = Side {
            u: elt_storage.u_face3_minus.get(),
            f: elt_storage.f_face3_minus.get(),
        };
        let plus = Side {
            u: elt_storage.u_face3_plus.get(),
            f: elt_storage.f_face3_plus.get(),
        };
        <GS::FS as FluxScheme>::flux_type(elt.face3.flux_key, minus, plus, elt.face3.outward_normal)
    };

    (face1_flux, face2_flux, face3_flux)
}
