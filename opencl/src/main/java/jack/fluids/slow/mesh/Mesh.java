package jack.fluids.slow.mesh;

import jack.fluids.slow.Point;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface Mesh {
  List<Segment> segments();

  // Is p inside the mesh, given that the origin point is outside?
  default boolean inside(Point p, Point origin) {
    Segment ray = Segment.of(p, origin);
    return segments().stream()
        .mapToInt(s -> ray.intersection(s).isPresent() ? 1 : 0)
        .sum() % 2 == 1;
  }
}
