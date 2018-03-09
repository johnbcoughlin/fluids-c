package jack.fluids;

import org.jocl.cl_mem;

import java.util.List;

public class GridGeometry {
  // one linear vertex buffer per solid object
  private final List<cl_mem> solidObjectVertexBuffers;

  // one linear buffer per solid object
  private final List<cl_mem> solidObjectBoundaryPointBuffers;

  // a full grid. each pixel is actually the closest point on the surface and distance to it
  private final cl_mem freeSurfaceSignedDistance;

  public GridGeometry(
      List<cl_mem> solidObjectVertexBuffers,
      List<cl_mem> solidObjectBoundaryPointBuffers,
      cl_mem freeSurfaceSignedDistance) {
    this.solidObjectVertexBuffers = solidObjectVertexBuffers;
    this.solidObjectBoundaryPointBuffers = solidObjectBoundaryPointBuffers;
    this.freeSurfaceSignedDistance = freeSurfaceSignedDistance;
  }
}
