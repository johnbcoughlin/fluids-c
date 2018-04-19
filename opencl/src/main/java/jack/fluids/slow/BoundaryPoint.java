package jack.fluids.slow;

public class BoundaryPoint {
  private final double x;
  private final double y;

  private double p;
  private double u;
  private double v;

  public BoundaryPoint(double x, double y) {
    this.x = x;
    this.y = y;
  }
}
