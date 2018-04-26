package jack.fluids.slow;

import jack.fluids.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;

public class State {
  private final int nx;
  private final double width;
  private final int ny;
  private final double height;
  private final INDArray p;
  private final INDArray u;
  private final INDArray v;

  public State(int nx,
               double width,
               int ny,
               double height,
               INDArray p,
               INDArray u,
               INDArray v) {
    this.nx = nx;
    this.width = width;
    this.ny = ny;
    this.height = height;
    this.p = p;
    this.u = u;
    this.v = v;
  }

  public INDArray p() {
    return p;
  }

  public INDArray u() {
    return u;
  }

  public INDArray v() {
    return v;
  }

  public Pair<Double, Double> toIndexSpace(double x, double y) {
    return Pair.of(x / width * nx, y / height * ny);
  }
}
