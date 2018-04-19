package jack.fluids.slow;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jack.fluids.util.Pair;

import javax.naming.ldap.Control;
import java.util.List;

public class State {
  private final int nx;
  private final double width;
  private final int ny;
  private final double height;
  private final Table<Integer, Integer, ControlVolume> pressureCells;
  private final List<BoundaryPoint> boundaryPoints;

  public State(int nx,
               double width,
               int ny,
               double height,
               Table<Integer, Integer, ControlVolume> pressureCells,
               List<BoundaryPoint> boundaryPoints) {
    this.nx = nx;
    this.width = width;
    this.ny = ny;
    this.height = height;
    this.pressureCells = pressureCells;
    this.boundaryPoints = boundaryPoints;
  }

  public Pair<Double, Double> toIndexSpace(double x, double y) {
    return Pair.of(x / width * nx, y / height * ny);
  }

  private Table<Integer, Integer, ControlVolume> uCells(Table<Integer, Integer, ControlVolume> pressureCells) {
    var result = HashBasedTable.create();
    for (int i = 0; i < nx; i++) {
      for (int j = 0; j < ny; j++) {
        if (pressureCells.contains(j, i) && pressureCells.contains(j, i+1)) {
          result.put(j, i, new UCell());
        }
      }
    }
  }
}
