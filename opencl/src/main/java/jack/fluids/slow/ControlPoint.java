package jack.fluids.slow;

import org.immutables.value.Value;

@Value.Immutable
public interface ControlPoint extends Point {
  @Value.Parameter
  double phi();

  static ControlPoint of(Point point, double phi) {
    return ImmutableControlPoint.of(phi, point.x(), point.y());
  }
}
