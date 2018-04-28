package jack.fluids.slow;

import org.immutables.value.Value;

@Value.Immutable
public interface StaggeredCellFace {
  Point point();

  default double distance(Point point) {
    return point().distance(point);
  }

  double area();

  /*
   * Info about, for example, u_E and u_W and their geometry as related to the interpolation of the velocity
   * component at this face.
   */
  boolean positiveDirectionFluid();
  double positiveDirectionDistance();
  double positivePositiveDirectionDistance();
  boolean negativeDirectionFluid();
  double negativeDirectionDistance();
  double negativeNegativeDirectionDistance();

  default double totalDistance() {
    return positiveDirectionDistance() + negativeDirectionDistance();
  }

  // interpolation coefficient
  default double theta() {
    return positiveDirectionDistance() / totalDistance();
  }

  /*
   * Info about v_ne and v_nw and their geometry as it relates to interpolation of the transverse velocity
   * component at this face.
   */
  boolean crosswisePositiveDirectionFluid();
  boolean crosswiseNegativeDirectionFluid();
  // interpolation coefficient
  double crosswiseTheta();
}
