package jack.fluids.slow;

import jack.fluids.slow.mesh.ImmutableMesh;
import jack.fluids.slow.mesh.Mesh;
import jack.fluids.slow.mesh.Segment;
import jack.fluids.slow.server.SlowServer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlowMain {
  public static void main(String[] args) {
    int nx = 10;
    int ny = 10;
    Mesh mesh = mesh(nx, ny, 1.2, 1.2);
    INDArray uCellsValue = new NDArray(nx + 1, ny);
    INDArray vCellsValus = new NDArray(nx, ny + 1);
    Grid grid = new Grid(nx, ny, 1.2, 1.2, uCellsValue, vCellsValus, mesh);

    for (int i = 0; i < grid.nx(); i++) {
      for (int j = 0; j < grid.ny(); j++) {
        Optional<Neighborhood> maybeNeighborhood = grid.uNeighborhood(i, j);
//        System.out.println(maybeNeighborhood);
      }
    }

    new SlowServer(grid).start();
  }

  private static Mesh mesh(int nx, int ny, double dx, double dy) {
    List<Segment> segments = new ArrayList<>();
    ImmutableMesh.Builder builder = ImmutableMesh.builder();
    double x = nx * dx;
    for (int i = 0; i < 10; i++) {
      double ax = i * (nx * dx / 10);
      double bx = (i + 1) * (nx * dx / 10);
      segments.add(Segment.of(
          Point.of(ax, (ax - x / 2) * (ax - x / 2) / x * 4),
          Point.of(bx, (bx - x / 2) * (bx - x / 2) / x * 4)
      ));
    }
    builder.addAllSegments(segments);
    builder.addSegments(Segment.of(
        segments.get(segments.size() - 1).b(),
        segments.get(0).a()
    ));
    return builder.build();
  }
}
