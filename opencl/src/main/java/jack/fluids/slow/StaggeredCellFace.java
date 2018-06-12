package jack.fluids.slow;

import jack.fluids.slow.mesh.Segment;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface StaggeredCellFace {
  default Point point() {
    return segment().midpoint();
  }

  Segment segment();

  default double distance(Point point) {
    return point().distance(point);
  }

  default double area() {
    return segment().length();
  }

  default boolean open() {
    return area() != 0.0;
  }

  /*
   * Info about, for example, u_E and u_W and their geometry as related to the interpolation of the velocity
   * component at this face.
   */
  double positiveDirectionDistance();
  double positivePositiveDirectionDistance();
  double negativeDirectionDistance();
  double negativeNegativeDirectionDistance();
}
