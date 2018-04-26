package jack.fluids.slow;

import org.immutables.value.Value;

@Value.Immutable
public interface Neighborhood {
  // Value of the velocity component at the center of the neighborhood/system of equations.
  double P();

  StaggeredCellFace n();
  StaggeredCellFace s();
  StaggeredCellFace e();
  StaggeredCellFace w();

  // Principal directions
  double N();
  double S();
  double E();
  double W();

  // Two cells away in each direction; used for QUICK
  double NN();
  double SS();
  double EE();
  double WW();

  // Oblique neighbors
  double ne();
  double nw();
  double se();
  double sw();

  default double he() { return 1.0; }
  default double hw() { return 1.0; }
  default double hn() { return 1.0; }
  default double hs() { return 1.0; }
}
