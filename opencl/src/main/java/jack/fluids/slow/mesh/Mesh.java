package jack.fluids.slow.mesh;

import jack.fluids.slow.Point;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface Mesh {
  List<Segment> segments();

  // TODO(jack) speed this up
  // Is p inside the mesh, given that the origin point is outside?
  default boolean inside(Point p, Point origin) {
    Segment ray = Segment.of(p, origin);
    return segments().stream()
        .mapToInt(s -> ray.intersection(s).isPresent() ? 1 : 0)
        .sum() % 2 == 1;
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
}
