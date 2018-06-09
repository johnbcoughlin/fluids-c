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
    double fAdv_n = 0.0;
    if (nb.fn().isPresent()) {
      StaggeredCellFace fn = nb.fn().get();
      double v_n = orthogonalAdvectingVelocity(fn, nb.nw().get(), nb.ne().get());
      double u_n = orthogonalAdvectedVelocity(fn, nb.E(), nb.P(), nb.EE(), nb.W(), v_n);
      double alpha_n = alpha(nb, fn, NORTH);
      fAdv_n = fn.area() * v_n * u_n * alpha_n;
    }

    double fAdv_s = 0.0;
    if (nb.fs().isPresent()) {
      StaggeredCellFace fs = nb.fs().get();
      double v_s = orthogonalAdvectingVelocity(fs, nb.sw().get(), nb.se().get());
      double u_s = orthogonalAdvectedVelocity(fs, nb.P(), nb.S(), nb.N(), nb.SS(), v_s);
      double alpha_s = alpha(nb, fs, SOUTH);
      fAdv_s = fs.area() * v_s * u_s * alpha_s;
    }

    double fAdv_e = 0.0;
    if (nb.fe().isPresent()) {
      StaggeredCellFace fe = nb.fe().get();
      double u_e = parallelAdvectedVelocity(fe, nb.E(), nb.P(), nb.EE(), nb.W());
      double alpha_e = alpha(nb, fe, EAST);
      fAdv_e = fe.area() * u_e * u_e * alpha_e * alpha_e;
    }

    double fAdv_w = 0.0;
    if (nb.fw().isPresent()) {
      StaggeredCellFace fw = nb.fw().get();
      double u_w = parallelAdvectedVelocity(fw, nb.P(), nb.W(), nb.E(), nb.WW());
      double alpha_w = alpha(nb, fw, WEST);
      fAdv_w = fw.area() * u_w * u_w * alpha_w * alpha_w;
    }

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
    if (!face.open()) {
      return 0.0;
    }
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
  public static double diffusiveFlux(Neighborhood nb) {
    double total = 0.0;
    if (nb.fn().isPresent()) {
      total += MU * nb.fn().get().area() * singleFaceGradient(nb.N(), nb.P());
    }
    if (nb.fs().isPresent()) {
      total += MU * nb.fs().get().area() * singleFaceGradient(nb.P(), nb.S());
    }
    if (nb.fe().isPresent()) {
      total += MU * nb.fe().get().area() * singleFaceGradient(nb.E(), nb.P());
    }
    if (nb.fw().isPresent()) {
      total += MU * nb.fw().get().area() * singleFaceGradient(nb.P(), nb.W());
    }
    return total;
  }


  private static double singleFaceGradient(ControlPoint pos, ControlPoint neg) {
    return (pos.phi() - neg.phi()) / (pos.distance(neg));
  }

  public static List<Term> diffusiveFluxTerms(Neighborhood nb) {
    ControlPoint P = nb.P();
    ControlPoint N = nb.N();
    ControlPoint S = nb.S();
    ControlPoint E = nb.E();
    ControlPoint W = nb.W();

    Term nTerm = new Term(N.variable(), nb.fn().isPresent()
        ? nb.fn().get().area() / N.distance(P) / 2 : 0.0);
    Term sTerm = new Term(S.variable(), nb.fs().isPresent()
        ? -nb.fs().get().area() / P.distance(S) / 2 : 0.0);
    Term eTerm = new Term(E.variable(), nb.fe().isPresent()
        ? nb.fe().get().area() / E.distance(P) / 2 : 0.0);
    Term wTerm = new Term(W.variable(), nb.fw().isPresent()
        ? -nb.fw().get().area() / P.distance(W) / 2 : 0.0);
    Term pTerm = new Term(P.variable(), (0.0
        - (nb.fn().isPresent() ? nb.fn().get().area() / N.distance(P) : 0.0)
        + (nb.fs().isPresent() ? nb.fs().get().area() / P.distance(S) : 0.0)
        - (nb.fe().isPresent() ? nb.fe().get().area() / E.distance(P) : 0.0)
        + (nb.fw().isPresent() ? nb.fw().get().area() / P.distance(W) : 0.0) / 2));

    return ImmutableList.of(pTerm, nTerm, sTerm, eTerm, wTerm);
  }
}
