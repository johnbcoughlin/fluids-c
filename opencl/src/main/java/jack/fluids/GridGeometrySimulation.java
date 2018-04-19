package jack.fluids;

import org.jocl.cl_mem;

import java.util.List;

public class GridGeometrySimulation {
  // one linear vertex buffer per solid object
  private final List<cl_mem> solidObjectVertexBuffers;

  // one linear buffer per solid object
  private final List<cl_mem> solidObjectBoundaryPointBuffers;

  // a full grid. each pixel is actually the closest point on the surface and distance to it
  private final cl_mem freeSurfaceSignedDistance;

  // crossing number parities of each point with respect to the solid boundaries.
  private final cl_mem solidBoundaryCrossingParities;

  // crossing number parities of each point with respect to the free surface boundaries
  private final cl_mem freeSurfaceCrossingParities;

  public GridGeometrySimulation(
      List<cl_mem> solidObjectVertexBuffers,
      List<cl_mem> solidObjectBoundaryPointBuffers,
      cl_mem freeSurfaceSignedDistance,
      cl_mem solidBoundaryCrossingParities,
      cl_mem freeSurfaceCrossingParities) {
    this.solidObjectVertexBuffers = solidObjectVertexBuffers;
    this.solidObjectBoundaryPointBuffers = solidObjectBoundaryPointBuffers;
    this.freeSurfaceSignedDistance = freeSurfaceSignedDistance;
    this.solidBoundaryCrossingParities = solidBoundaryCrossingParities;
    this.freeSurfaceCrossingParities = freeSurfaceCrossingParities;
  }
}
