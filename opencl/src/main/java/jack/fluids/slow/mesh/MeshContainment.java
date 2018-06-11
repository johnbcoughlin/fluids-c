package jack.fluids.slow.mesh;

import jack.fluids.slow.Point;

public class MeshContainment {
  public static boolean contains(Mesh mesh, Point point, Point origin) {
    return (mesh.segments().stream()
        .mapToInt(segment -> triangleContains(point, segment, origin))
        .sum() / 2) % 2 != 0;
  }

  /**
   * Return 0 if the triangle does not contain the point, 2 if it does, and 1 if the point is
   * on the boundary.
   */
  public static int triangleContains(Point point, Segment segment, Point origin) {
    // first come up with a stable ordering of the points.
    // p.x() <= q.x() && p.y() <= q.y().
    Point p;
    Point q;
    if (segment.a().x() < segment.b().x()) {
      p = segment.a();
      q = segment.b();
    } else if (segment.a().x() > segment.b().x()) {
      p = segment.b();
      q = segment.a();
    } else if (segment.a().y() <= segment.b().y()) {
      p = segment.a();
      q = segment.b();
    } else {
      p = segment.b();
      q = segment.a();
    }

    double originToPCrossOriginToPoint = p.minus(origin).cross(point.minus(origin));
    double pToQCrossPToPoint = q.minus(p).cross(point.minus(p));
    double qToOriginCrossQToPoint = origin.minus(q).cross(point.minus(q));

    // If the point lies directly on the segment, count it fully.
    if (pToQCrossPToPoint == 0.0) {
      return 2;
      // if the point lies on both rays of the triangle, then count it 0 times
    } else if (originToPCrossOriginToPoint == 0.0 && qToOriginCrossQToPoint == 0.0) {
      return 0;
      // if the point lies on one ray of the triangle, count it half a time
    } else if (originToPCrossOriginToPoint == 0.0 || qToOriginCrossQToPoint == 0.0) {
      return 1;
      // if all the indicators are the same sign, then we must be inside
    } else if (originToPCrossOriginToPoint > 0.0 && pToQCrossPToPoint > 0.0 &&
        qToOriginCrossQToPoint > 0.0) {
      return 2;
    } else if (originToPCrossOriginToPoint < 0.0 && pToQCrossPToPoint < 0.0 &&
        qToOriginCrossQToPoint < 0.0) {
      return 2;
    } else {
      return 0;
    }
  }
}
