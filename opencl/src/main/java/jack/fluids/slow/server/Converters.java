package jack.fluids.slow.server;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import jack.fluids.slow.Grid;
import jack.fluids.slow.Neighborhood;
import jack.fluids.slow.mesh.Mesh;

import java.io.StringWriter;

public class Converters {
  public static String toJson(Grid grid, Gson gson) throws Exception {
    StringWriter writer = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(writer).beginObject();
    jsonWriter
        .name("nx").value(grid.nx())
        .name("ny").value(grid.ny())
        .name("dx").value(grid.dx())
        .name("dy").value(grid.dy());
    jsonWriter.name("mesh");
    gson.toJson(grid.mesh, Mesh.class, jsonWriter);
    jsonWriter.name("u_neighborhoods");
    jsonWriter.beginArray();
    for (int i = 0; i < grid.nx(); i++) {
      for (int j = 0; j < grid.ny(); j++) {
        grid.uNeighborhood(i, j).ifPresent(nb ->
            gson.toJson(nb, Neighborhood.class, jsonWriter));
      }
    }
    jsonWriter.endArray();
    jsonWriter.endObject();
    return writer.toString();
  }
}
