package jack.fluids.slow;

import org.immutables.value.Value;

public interface Point {
  @Value.Parameter
  double x();

  @Value.Parameter
  double y();

  default Point minus(Point other) {
    return Point.of(x() - other.x(), y() - other.y());
  }

  default Point plus(Point other) {
    return Point.of(x() + other.x(), y() + other.y());
  }

  default Point times(double scalar) {
    return Point.of(scalar * x(), scalar * y());
  }

  default double cross(Point other) {
    return x() * other.y() - y() * other.x();
  }

  default double distance(Point other) {
    double dx = x() - other.x();
    double dy = y() - other.y();
    return Math.sqrt(dx * dx + dy + dy);
  }

  static Point of(double x, double y) {
    return ImmutablePointVal.of(x, y);
  }
}
