package jack.fluids.slow;

import jack.fluids.slow.mesh.ImmutableMesh;
import jack.fluids.slow.mesh.Mesh;
import jack.fluids.slow.mesh.Segment;
import org.assertj.core.data.Offset;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class GridsTest {
  @Test
  public void uControlPointLocation_bothInside_twoIntersections() {
    Mesh mesh = ImmutableMesh.builder()
        .addSegments(Segment.of(Point.of(0.3, 0.2), Point.of(0.6, 1.0)))
        .addSegments(Segment.of(Point.of(0.6, 1.0), Point.of(0.3, 1.2)))
        .build();
    Optional<Point> actual = Grids.uControlPointLocation(1.0, 1.0, 1, 1, mesh);
    assertThat(actual).contains(Point.of(0.5, 0.9));
  }

  @Test
  public void uControlPointLocation_bothInside_noIntersections() {
    Mesh mesh = ImmutableMesh.builder()
        .addSegments(Segment.of(Point.of(0.3, 0.2), Point.of(0.3, 1.2)))
        .build();
    Optional<Point> actual = Grids.uControlPointLocation(1.0, 1.0, 1, 1, mesh);
    assertThat(actual).isEmpty();
  }

  @Test
  public void uControlPointLocation_oneInside() {
    Mesh mesh = ImmutableMesh.builder()
        .addSegments(Segment.of(Point.of(0.3, 0.2), Point.of(0.6, 1.0)))
        .build();
    Optional<Point> actual = Grids.uControlPointLocation(1.0, 1.0, 1, 1, mesh);
    assertThat(actual.get().y()).isEqualTo(1.11666, Offset.offset(0.0001));
  }
}