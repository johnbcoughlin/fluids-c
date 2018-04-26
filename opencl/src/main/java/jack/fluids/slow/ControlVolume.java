package jack.fluids.slow;

import java.util.List;

public abstract class ControlVolume {
  // coordinates of the center of the control volume
  private final double x;
  private final double y;
  private final List<Face> faces;

  // primitive var at the center of the control volume
  protected final String variableName;

  public ControlVolume(double x, double y, List<Face> faces, String variableName) {
    this.x = x;
    this.y = y;
    this.faces = faces;
    this.variableName = variableName;
  }

  /**
   * Return the linear equation resulting from the local discretization of the control volume conservation equation.
   *
   * This equation forms part of a system of equations for the current time step.
   * It's up to the smoother to decide how it will relax this--individually or collectively with other equations.
   */
  protected abstract LinearEquation equation(State n, State n_1);
}
