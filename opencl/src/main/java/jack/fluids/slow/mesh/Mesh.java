package jack.fluids.slow.mesh;

import jack.fluids.slow.Point;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Gson.TypeAdapters
public interface Mesh {
  List<Segment> segments();

  // TODO(jack) speed this up
  // Is p inside the mesh, given that the origin point is outside?
  default boolean inside(Point p, Point origin) {
    return MeshContainment.contains(this, p, origin);
  }

  // return the point nearest s.a() which intersects the mesh, if there is one
  default Optional<Point> intersectionPoint(Segment test) {
    return segments().stream()
        .map(s -> test.intersection(s))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .sorted(Comparator.comparing(p -> p.distance(test.a())))
        .findFirst();
  }

  default double minimumDistance(Point point) {
    return segments().stream()
        .mapToDouble(s -> distance(s, point))
        .min().orElse(Double.MAX_VALUE);
  }

  static double distance(Segment segment, Point point) {
    double l2 = segment.length() * segment.length();
    if (l2 == 0.0) {
      return point.distance(segment.a());
    }
    Point s = segment.b().minus(segment.a());
    Point x = point.minus(segment.a());
    double dot = s.x() * x.x() + x.y() * x.y();
    // the projection of point onto the line extending segment, parameterized, clamped to a (0, 1)
    double t = Math.max(0, Math.min(1, dot / l2));
    Point projection = segment.a().plus(s.times(t));
    return point.distance(projection);
  }
}
