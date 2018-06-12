package jack.fluids.slow.mesh;

import jack.fluids.slow.Point;
import jack.fluids.slow.PointVal;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Gson.TypeAdapters
public interface Segment {
  @Value.Parameter
  @Gson.ExpectedSubtypes({
      PointVal.class
  })
  Point a();

  @Value.Parameter
  @Gson.ExpectedSubtypes({
      PointVal.class
  })
  Point b();

  default Point eastmost() {
    return a().x() > b().x() ? a() : b();
  }

  default Point westmost() {
    return a().x() < b().x() ? a() : b();
  }

  static Segment of(Point a, Point b) {
    return ImmutableSegment.of(a, b);
  }

  // with thanks to https://stackoverflow.com/a/565282/1319631
  default Optional<Point> intersection(Segment other) {
    Point s = b().minus(a());
    Point r = other.b().minus(other.a());
    double cross = r.cross(s);
    if (cross == 0) {
      // lines are parallel
      return Optional.empty();
    }
    double t = (a().minus(other.a())).cross(s) / cross;
    double u = (a().minus(other.a())).cross(r) / cross;
    if (0 <= t && t <= 1 && 0 <= u && u <= 1) {
      return Optional.of(a().plus(s.times(u)));
    }
    return Optional.empty();
  }

  default Point midpoint() {
    return a().plus(b()).times(0.5);
  }

  default double length() {
    return a().distance(b());
  }
}
