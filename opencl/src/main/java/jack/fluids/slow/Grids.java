package jack.fluids.slow;

import com.google.common.collect.ImmutableList;
import jack.fluids.slow.coords.VCellCoords;
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

  /**
   * Return the natural location of a u control point with the given integer coordinates
   */
  public static Point uPointAtCoords(double dx, double dy, int i, int j) {
    return Point.of((i - 0.5) * dx, j * dy);
  }

  /**
   * Compute the longest sub-segment of the given segment which lies fully inside the mesh.
   */
  public static Optional<Segment> principalSegmentLocation(Segment naturalSegment, Mesh mesh) {
    List<Point> allIntersections = mesh.segments().stream()
        .map(s -> s.intersection(naturalSegment))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .sorted(Comparator.comparingDouble(Point::y))
        .collect(Collectors.toList());
    List<Point> controlPoints = ImmutableList.<Point>builder()
        .add(naturalSegment.a())
        .addAll(allIntersections)
        .add(naturalSegment.b())
        .build();

    Point origin = Point.of(0, 0);
    boolean aInside = mesh.inside(naturalSegment.a(), origin);
    boolean bInside = mesh.inside(naturalSegment.b(), origin);
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
        .max(Comparator.comparingDouble(Segment::length).reversed());
  }

  /**
   * Compute the largest segment of a grid line which is inside the mesh
   */
  public static Optional<Segment> pCellUFacePrincipalSegment(double dx, double dy, int i, int j,
      Mesh mesh) {
    Point a = Point.of((i - 0.5) * dx, (j - 0.5) * dy);
    Point b = Point.of((i - 0.5) * dx, (j + 0.5) * dy);
    Segment naturalSegment = Segment.of(a, b);
    return principalSegmentLocation(naturalSegment, mesh);
  }

  public static Segment pCellVFace(double dx, double dy, VCellCoords coords) {
    int i = coords.i();
    int j = coords.j();
    return Segment.of(
        Point.of((i - 0.5) * dx, (j - 0.5) * dy),
        Point.of((i + 0.5) * dx, (j - 0.5) * dy)
    );
  }

  /**
   * @param i the i coordinate of the u cell
   * @param j the j coordinate of the u cell
   */
  public static Segment uCellEastWall(double dx, double dy, int i, int j) {
    Point a = Point.of(i * dx, (j - 0.5) * dy);
    Point b = Point.of(i * dx, (j + 0.5) * dy);
    return Segment.of(a, b);
  }

  /**
   * @param i the i coordinate of the u cell
   * @param j the j coordinate of the u cell
   */
  public static Segment uCellWestWall(double dx, double dy, int i, int j) {
    Point a = Point.of((i - 1) * dx, (j - 0.5) * dy);
    Point b = Point.of((i - 1) * dx, (j + 0.5) * dy);
    return Segment.of(a, b);
  }

  /**
   * @param i the i coordinate of the u cell
   * @param j the j coordinate of the u cell
   */
  public static Segment uCellNorthWall(double dx, double dy, int i, int j) {
    return Segment.of(
        Point.of((i - 1) * dx, (j + 0.5) * dy),
        Point.of(i * dx, (j + 0.5) * dy)
    );
  }

  /**
   * @param i the i coordinate of the u cell
   * @param j the j coordinate of the u cell
   */
  public static Segment uCellSouthWall(double dx, double dy, int i, int j) {
    return Segment.of(
        Point.of((i - 1) * dx, (j - 0.5) * dy),
        Point.of(i * dx, (j - 0.5) * dy)
    );
  }

  /**
   * TODO(jack) find a better algorithm for this.
   *
   * @param x0 the x origin of the cell
   * @param y0 the y origin of the cell
   * @return an approximation of the volume of the cell
   */
  public static double approximateVolume(double dx, double dy,
      double x0, double y0, Mesh mesh) {
    int count = 0;
    Point origin = Point.of(0, 0);
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        Point p = Point.of(x0 + i * dx / 10, y0 + j * dy / 10);
        if (mesh.inside(p, origin)) {
          count++;
        }
      }
    }
    return count * dx * dy / 100;
  }
}
