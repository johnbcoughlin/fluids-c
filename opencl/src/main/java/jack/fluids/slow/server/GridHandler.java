package jack.fluids.slow.server;

import com.google.gson.Gson;
import jack.fluids.slow.Grid;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class GridHandler extends AbstractHandler {
  private final Grid grid;
  private final Gson gson;

  public GridHandler(Grid grid, Gson gson) {
    this.grid = grid;
    this.gson = gson;
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    response.addHeader("Access-Control-Allow-Origin", "*");
    try (PrintWriter writer = response.getWriter()) {
      writer.print(Converters.toJson(grid, gson));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
