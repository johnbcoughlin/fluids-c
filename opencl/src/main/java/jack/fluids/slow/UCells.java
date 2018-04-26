package jack.fluids.slow;

import jack.fluids.slow.LinearEquation.Term;

import java.util.ArrayList;
import java.util.List;

import static jack.fluids.slow.Grid.MU;

public class UCells {
  protected static LinearEquation equation(State n, State n_1, int i, int j) {
    return null;
  }

  /**
   * @return the sum of the advective flux across all cell faces
   */
  public static double advectiveFlux(Neighborhood neighborhood) {

  }

  private static double advectedVelocity(StaggeredCellFace face, double posValue, double negValue,
                                         double posPosValue, double negNegValue) {
    double pos = face.positiveDirectionFluid() ? posValue : 0.0;
    double neg = face.negativeDirectionFluid() ? negValue : 0.0;
    double posPos = face.positiveDirectionFluid() ? posPosValue : 0.0;
    double negNeg = face.negativeDirectionFluid() ? negNegValue : 0.0;

    double firstOrderApprox = face.theta() * pos + (1.0 - face.theta()) * neg;

    double thirdOrderApprox;
    if (firstOrderApprox > 0.0) {
      
    }
  }

  /**
   * @return the sum of the diffusive flux across all cell faces
   */
  public static double diffusiveFlux(Neighborhood neighborhood) {
    double total = 0.0;
    total += MU * neighborhood.n().area() * singleFaceGradient(neighborhood.n(), neighborhood.N(), neighborhood.P());
    total += MU * neighborhood.s().area() * singleFaceGradient(neighborhood.s(), neighborhood.P(), neighborhood.S());
    total += MU * neighborhood.e().area() * singleFaceGradient(neighborhood.e(), neighborhood.E(), neighborhood.P());
    total += MU * neighborhood.w().area() * singleFaceGradient(neighborhood.w(), neighborhood.P(), neighborhood.W());
    return total;
  }

  private static double singleFaceGradient(StaggeredCellFace face, double posValue, double negValue) {
    double pos = face.positiveDirectionFluid() ? posValue : 0.0;
    double neg = face.negativeDirectionFluid() ? negValue : 0.0;
    return (pos - neg) / face.totalDistance();
  }

  private static List<Term> diffusiveFlux_abandoned(int i, int j, Direction direction, Grid grid) {
    String centerId = unknownId(i, j);
    List<Term> result = new ArrayList<>();
    switch (direction) {
      case NORTH:
        if (grid.uNorthmost(j)) {
          return result;
        }
        String northId = unknownId(i, j+1);
        result.add(new Term(northId, 1.0));
        result.add(new Term(centerId, -1.0));
        return result;
      case SOUTH:
        if (grid.uSouthmost(j)) {
          return result;
        }
        String southId = unknownId(i, j-1);
        result.add(new Term(centerId, 1.0));
        result.add(new Term(southId, -1.0));
        return result;
      case EAST:
        if (grid.uEastmost(i)) {
          return result;
        }
        String eastId = unknownId(i+1, j);
        result.add(new Term(eastId, 1.0));
        result.add(new Term(centerId, -1.0));
        return result;
      case WEST:
        if (grid.uWestmost(i)) {
          return result;
        }
        String westId = unknownId(i-1, j);
        result.add(new Term(centerId, 1.0));
        result.add(new Term(westId, -1.0));
        return result;
      default:
        throw new IllegalStateException("come on");
    }
  }

  public static double advectiveFlux_abandoned(int i, int j, Direction direction, Grid grid, State state) {
  }

  public static double advectiveFluxEast_abandoned(int i, int j, Grid grid, State state) {
    double u = state.u().getDouble(i, j);
    if (u < 0) {
      // we're in the position of using P, E, EE

    }
    return 0;
  }

  public static String unknownId(int i, int j) {
    return String.format("u-%d.%d", i, j);
  }

}
