package jack.fluids.slow;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Value.Immutable
@Gson.TypeAdapters
public interface Neighborhood {
  // Volume of the central control volume.
  double volume();

  // Value of the velocity component at the center of the neighborhood/system of equations.
  ControlPoint p();

  Optional<StaggeredCellFace> fn();
  Optional<StaggeredCellFace> fs();
  Optional<StaggeredCellFace> fe();
  Optional<StaggeredCellFace> fw();

  // Principal directions
  ControlPoint n();
  ControlPoint s();
  ControlPoint e();
  ControlPoint w();

  // Two cells away in each direction; used for QUICK
  ControlPoint nn();
  ControlPoint ss();
  ControlPoint ee();
  ControlPoint ww();

  // Oblique neighbors. If p is a u-control point, then these are v-control points, and vice versa
  Optional<ControlPoint> ne();
  Optional<ControlPoint> nw();
  Optional<ControlPoint> se();
  Optional<ControlPoint> sw();

  Optional<BoundaryDistances> boundaryDistances();

  @Value.Check
  default void check() {
    if (fn().isPresent()) {
      checkArgument(ne().isPresent() && nw().isPresent(),
          "both oblique neighbors must be present");
    }
    if (fs().isPresent()) {
      checkArgument(se().isPresent() && sw().isPresent(),
          "both oblique neighbors must be present");
    }
  }

  // Vertical distances from the corresponding ControlPoints to the boundary.
  @Value.Immutable
  interface BoundaryDistances {
    // for the center cell
    double hP();

    // for the immediate neighbor cells
    double hN();
    double hS();
    double hE();
    double hW();

    // for the centers of the cell faces
    double hn();
    double hs();
    double he();
    double hw();
  }
}
