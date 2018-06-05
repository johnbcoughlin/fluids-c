package jack.fluids.slow;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public interface Neighborhood {
  // Volume of the central control volume.
  double volume();

  // Value of the velocity component at the center of the neighborhood/system of equations.
  ControlPoint P();

  StaggeredCellFace fn();
  StaggeredCellFace fs();
  StaggeredCellFace fe();
  StaggeredCellFace fw();

  // Principal directions
  ControlPoint N();
  ControlPoint S();
  ControlPoint E();
  ControlPoint W();

  // Two cells away in each direction; used for QUICK
  ControlPoint NN();
  ControlPoint SS();
  ControlPoint EE();
  ControlPoint WW();

  // Oblique neighbors. If P is a u-control point, then these are v-control points, and vice versa
  ControlPoint ne();
  ControlPoint nw();
  ControlPoint se();
  ControlPoint sw();

  Optional<BoundaryDistances> boundaryDistances();

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
