package jack.fluids.slow;

public class NeighborhoodGeometry {
  private NeighborhoodGeometry() {}

  public static double alpha(Neighborhood nb, Direction direction) {
    if (!nb.boundaryDistances().isPresent()) {
      return 1.0;
    }
    Neighborhood.BoundaryDistances bd = nb.boundaryDistances().get();
    Point face;
    double fD;
    double H;
    double h;
    switch (direction) {
      case NORTH:
        face = nb.fn().point();
        fD = face.distance(nb.N());
        H = bd.hN();
        h = bd.he();
        break;
      case SOUTH:
        face = nb.fs().point();
        fD = face.distance(nb.S());
        H = bd.hS();
        h = bd.hs();
        break;
      default:
        throw new RuntimeException();
    }
    double Pf = nb.P().distance(face);
    double PD = Pf + fD;
    double interp = (bd.hP() * fD + H * Pf) / PD;
    return h / interp;
  }
}
