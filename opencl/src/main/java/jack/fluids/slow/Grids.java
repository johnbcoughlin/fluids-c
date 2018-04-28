package jack.fluids.slow;

import com.google.common.collect.ImmutableList;
import jack.fluids.slow.mesh.Mesh;
import jack.fluids.slow.mesh.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Grids {
  private static final Logger logger = LoggerFactory.getLogger(Grids.class);

  public static Optional<Point> uControlPointLocation(double dx, double dy, int i, int j, Mesh mesh) {
    Point a = Point.of(i - 0.5 * dx, j - 0.5 * dy);
    Point b = Point.of(i - 0.5 * dx, j + 0.5 * dy);
    Segment naturalSegment = Segment.of(a, b);
    List<Point> allIntersections = mesh.segments().stream()
        .map(s -> s.intersection(naturalSegment))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .sorted(Comparator.comparingDouble(Point::y))
        .collect(Collectors.toList());
    List<Point> controlPoints = ImmutableList.<Point>builder()
        .add(a)
        .addAll(allIntersections)
        .add(b)
        .build();

    Point origin = Point.of(0, 0);
    boolean aInside = mesh.inside(a, origin);
    boolean bInside = mesh.inside(b, origin);
    if (!aInside && !bInside) {
      if (allIntersections.isEmpty()) {
        return Optional.empty();
      } else if (allIntersections.size() % 2 == 1) {
        // don't know what happened here, but return empty.
        logger.warn("Something weird happened with the mesh intersection parity");
        return Optional.empty();
      }
    }
    List<Segment> openSegments = new ArrayList<>();
    int k = aInside ? 1 : 0;
    while (k < controlPoints.size() - 1) {
      openSegments.add(Segment.of(controlPoints.get(k), controlPoints.get(k + 1)));
      k += 2;
    }
    if (openSegments.isEmpty()) {
      return Optional.empty();
    }
    return openSegments.stream()
        .max(Comparator.comparingDouble(Segment::length).reversed())
        .map(Segment::midpoint);
  }
}
