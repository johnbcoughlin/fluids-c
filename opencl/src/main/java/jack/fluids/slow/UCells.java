package jack.fluids.slow;

import com.google.common.collect.ImmutableList;
import jack.fluids.slow.LinearEquation.Term;

import java.util.List;

import static jack.fluids.slow.Direction.EAST;
import static jack.fluids.slow.Direction.NORTH;
import static jack.fluids.slow.Direction.SOUTH;
import static jack.fluids.slow.Direction.WEST;
import static jack.fluids.slow.Grid.MU;
import static jack.fluids.slow.NeighborhoodGeometry.alpha;

public class UCells {
  public static LinearEquation localEquation(double dt, Neighborhood nb, Neighborhood n,
      double del_p_del_y_n_1_1, Neighborhood n_1) {
    Term vTerm = new Term(nb.P().variable(), nb.volume() / dt);
    List<Term> diffusiveFluxTerms = diffusiveFluxTerms(nb);
    double rhs = -nb.volume() / dt * n.P().phi();
    rhs += 3.0 / 2 * advectiveFluxU(n) - 1.0 / 2 * advectiveFluxU(n_1);
    rhs += nb.volume() * del_p_del_y_n_1_1;
    rhs += 1.0 / 2 * diffusiveFlux(n);
    return new LinearEquation(ImmutableList.<Term>builder()
        .add(vTerm)
        .addAll(diffusiveFluxTerms)
        .build(), rhs);
  }

  /**
   * @return the sum of the advective flux across all cell faces
   */
  public static double advectiveFluxU(Neighborhood nb) {
    double v_n = orthogonalAdvectingVelocity(nb.fn(), nb.nw(), nb.ne());
    double u_n = orthogonalAdvectedVelocity(nb.fn(), nb.E(), nb.P(), nb.EE(), nb.W(), v_n);
    double alpha_n = alpha(nb, NORTH);
    double fAdv_n = nb.fn().area() * v_n * u_n * alpha_n;

    double v_s = orthogonalAdvectingVelocity(nb.fs(), nb.sw(), nb.se());
    double u_s = orthogonalAdvectedVelocity(nb.fs(), nb.P(), nb.S(), nb.N(), nb.SS(), v_s);
    double alpha_s = alpha(nb, SOUTH);
    double fAdv_s = nb.fs().area() * v_s * u_s * alpha_s;

    double u_e = parallelAdvectedVelocity(nb.fe(), nb.E(), nb.P(), nb.EE(), nb.W());
    double alpha_e = alpha(nb, EAST);
    double fAdv_e = nb.fe().area() * u_e * u_e * alpha_e * alpha_e;

    double u_w = parallelAdvectedVelocity(nb.fw(), nb.P(), nb.W(), nb.E(), nb.WW());
    double alpha_w = alpha(nb, WEST);
    double fAdv_w = nb.fw().area() * u_w * u_w * alpha_w * alpha_w;

    return fAdv_n + fAdv_s + fAdv_e + fAdv_w;
  }

  private static double parallelAdvectedVelocity(StaggeredCellFace face,
      ControlPoint pos, ControlPoint neg,
      ControlPoint posPos, ControlPoint negNeg) {
    double interiorDistance = face.distance(neg) + face.distance(pos);
    double centralDifferenceApprox = (face.distance(neg) * pos.phi() + face.distance(pos) * neg
        .phi())
        / interiorDistance;
    double quickApprox;
    // quantities are named here according to https://people.eng.unimelb.edu
    // .au/imarusic/proceedings/12/LiY.pdf
    if (centralDifferenceApprox > 0.0) {
      double delta2 = face.positiveDirectionDistance();
      double delta3 = face.negativeNegativeDirectionDistance();
      double exteriorDistance = delta2 + delta3;
      double jump = exteriorDistance - interiorDistance;
      double beta1 = interiorDistance / jump;
      double beta2 = exteriorDistance / jump;
      double kappa = pos.phi() - beta2 * neg.phi() + beta1 * negNeg.phi();
      double q = delta2 / exteriorDistance;
      quickApprox = centralDifferenceApprox - q * kappa;
    } else {
      double delta2 = face.negativeDirectionDistance();
      double delta3 = face.positivePositiveDirectionDistance();
      double exteriorDistance = delta2 + delta3;
      double jump = exteriorDistance - interiorDistance;
      double beta1 = interiorDistance / jump;
      double beta2 = exteriorDistance / jump;
      double kappa = neg.phi() - beta2 * pos.phi() + beta1 * posPos.phi();
      double q = delta2 / exteriorDistance;
      quickApprox = centralDifferenceApprox - q * kappa;
    }
    return quickApprox;
  }

  private static double orthogonalAdvectingVelocity(StaggeredCellFace face, ControlPoint neg,
      ControlPoint pos) {
    double negDistance = face.point().distance(neg);
    double posDistance = face.point().distance(pos);
    double totalDistance = negDistance + posDistance;
    return (pos.phi() * negDistance + neg.phi() * posDistance) / totalDistance;
  }

  private static double orthogonalAdvectedVelocity(StaggeredCellFace face,
      ControlPoint pos, ControlPoint neg,
      ControlPoint posPos, ControlPoint negNeg,
      double advectingVelocity) {
    double interiorDistance = face.distance(neg) + face.distance(pos);
    double centralDifferenceApprox = (face.distance(neg) * pos.phi() + face.distance(pos) * neg
        .phi())
        / interiorDistance;
    double quickApprox;
    if (advectingVelocity > 0.0) {
      double delta2 = face.distance(pos);
      double delta3 = face.negativeNegativeDirectionDistance();
      double exteriorDistance = delta2 + delta3;
      double jump = exteriorDistance - interiorDistance;
      double beta1 = interiorDistance / jump;
      double beta2 = exteriorDistance / jump;
      double kappa = pos.phi() - beta2 * neg.phi() + beta1 * negNeg.phi();
      double q = delta2 / exteriorDistance;
      quickApprox = centralDifferenceApprox - q * kappa;
    } else {
      double delta2 = face.negativeDirectionDistance();
      double delta3 = face.positivePositiveDirectionDistance();
      double exteriorDistance = delta2 + delta3;
      double jump = exteriorDistance - interiorDistance;
      double beta1 = interiorDistance / jump;
      double beta2 = exteriorDistance / jump;
      double kappa = neg.phi() - beta2 * pos.phi() + beta1 * posPos.phi();
      double q = delta2 / exteriorDistance;
      quickApprox = centralDifferenceApprox - q * kappa;
    }
    return quickApprox;
  }

  /**
   * @return the sum of the diffusive flux across all cell faces
   */
  public static double diffusiveFlux(Neighborhood neighborhood) {
    double total = 0.0;
    total += MU * neighborhood.fn().area() * singleFaceGradient(neighborhood.N(), neighborhood.P());
    total += MU * neighborhood.fs().area() * singleFaceGradient(neighborhood.P(), neighborhood.S());
    total += MU * neighborhood.fe().area() * singleFaceGradient(neighborhood.E(), neighborhood.P());
    total += MU * neighborhood.fw().area() * singleFaceGradient(neighborhood.P(), neighborhood.W());
    return total;
  }


  private static double singleFaceGradient(ControlPoint pos, ControlPoint neg) {
    return (pos.phi() - neg.phi()) / (pos.distance(neg));
  }

  public static List<Term> diffusiveFluxTerms(Neighborhood neighborhood) {
    ControlPoint P = neighborhood.P();
    ControlPoint N = neighborhood.N();
    ControlPoint S = neighborhood.S();
    ControlPoint E = neighborhood.E();
    ControlPoint W = neighborhood.W();

    Term nTerm = new Term(N.variable(), neighborhood.fn().area() / N.distance(P) / 2);
    Term sTerm = new Term(S.variable(), -neighborhood.fs().area() / P.distance(S) / 2);
    Term eTerm = new Term(E.variable(), neighborhood.fe().area() / E.distance(P) / 2);
    Term wTerm = new Term(W.variable(), -neighborhood.fw().area() / P.distance(W) / 2);
    Term pTerm = new Term(P.variable(), (-neighborhood.fn().area() / N.distance(P)
        + neighborhood.fs().area() / P.distance(S)
        - neighborhood.fe().area() / E.distance(P)
        + neighborhood.fw().area() / P.distance(W)) / 2);

    return ImmutableList.of(pTerm, nTerm, sTerm, eTerm, wTerm);
  }
}
