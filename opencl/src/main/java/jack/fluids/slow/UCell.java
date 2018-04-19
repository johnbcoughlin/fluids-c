package jack.fluids.slow;

import java.util.List;

public class UCell extends ControlVolume {
  public UCell(double x, double y, List<Face> faces, String variableName) {
    super(x, y, faces, variableName);
  }

  @Override
  protected LinearEquation equation(State n, State n_1) {
    return null;
  }
}
