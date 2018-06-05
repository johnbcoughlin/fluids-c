package jack.fluids.slow;

import org.immutables.value.Value;

@Value.Immutable
public interface ControlPoint extends Point {
  @Value.Parameter
  double phi();

  /**
   * If this is empty, then the control point represents, say, a boundary point, which
   * is not actually an unknown value.
   *
   * In effect, a term containing the "" variable is a constant term.
   */
  @Value.Parameter
  String variable();

  static ControlPoint of(Point point, String variable, double phi) {
    return ImmutableControlPoint.of(phi, variable, point.x(), point.y());
  }
}
