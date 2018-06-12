package jack.fluids.slow.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jack.fluids.slow.Grid;
import jack.fluids.slow.GsonAdaptersControlPoint;
import jack.fluids.slow.GsonAdaptersNeighborhood;
import jack.fluids.slow.GsonAdaptersPointVal;
import jack.fluids.slow.GsonAdaptersStaggeredCellFace;
import jack.fluids.slow.mesh.GsonAdaptersMesh;
import jack.fluids.slow.mesh.GsonAdaptersSegment;
import org.eclipse.jetty.server.Server;

public class SlowServer {
  private final Grid grid;

  private Server server;

  public SlowServer(Grid grid) {
    this.grid = grid;
  }

  public void start() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(new GsonAdaptersNeighborhood())
        .registerTypeAdapterFactory(new GsonAdaptersSegment())
        .registerTypeAdapterFactory(new GsonAdaptersMesh())
        .registerTypeAdapterFactory(new GsonAdaptersPointVal())
        .registerTypeAdapterFactory(new GsonAdaptersControlPoint())
        .registerTypeAdapterFactory(new GsonAdaptersStaggeredCellFace())
        .create();
    server = new Server(8080);

    server.setHandler(new GridHandler(grid, gson));

    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
