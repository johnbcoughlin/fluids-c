package jack.fluids.slow;

public class NeighborhoodGeometry {
  private NeighborhoodGeometry() {}

  public static double alpha(Neighborhood nb, StaggeredCellFace face, Direction direction) {
    if (!nb.boundaryDistances().isPresent()) {
      return 1.0;
    }
    Neighborhood.BoundaryDistances bd = nb.boundaryDistances().get();
    double fD;
    double H;
    double h;
    switch (direction) {
      case NORTH:
        fD = face.distance(nb.n());
        H = bd.hN();
        h = bd.he();
        break;
      case SOUTH:
        fD = face.distance(nb.s());
        H = bd.hS();
        h = bd.hs();
        break;
      default:
        throw new RuntimeException();
    }
    double Pf = face.distance(nb.p());
    double PD = Pf + fD;
    double interp = (bd.hP() * fD + H * Pf) / PD;
    return h / interp;
  }
}
