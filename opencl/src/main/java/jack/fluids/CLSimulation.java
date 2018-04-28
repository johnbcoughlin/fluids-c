package jack.fluids;

import com.google.common.collect.ImmutableList;
import com.jogamp.opengl.GL4;
import jack.fluids.buffers.FloatBuffer1D;
import jack.fluids.buffers.SplitBuffer;
import jack.fluids.cl.Session;
import jack.fluids.kernels.ComputeSolidObjectBoundariesKernel;
import org.jocl.cl_mem;

import java.util.List;

public class CLSimulation {
  public static final long CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE = 0x10000000L;

  private final GL4 gl;
  private final Session session;
  private final int nx;
  private final int ny;

  private GridGeometrySimulation gridGeometrySimulation;

  public CLSimulation(
      GL4 gl,
      Session session,
      int nx,
      int ny
      ) {
    this.gl = gl;
    this.session = session;
    this.nx = nx;
    this.ny = ny;
  }

  public void init() {
    float[] triangle = new float[] {
        .12f * nx, .12f * ny,
        .78f * nx, .12f * ny,
        .78f * nx, .12f * ny,
        .56f * nx, .84f * ny,
        .56f * nx, .84f * ny,
        .12f * nx, .12f * ny
    };
    List<cl_mem> solidObjectVBOs = ImmutableList.of(session.createFloatBuffer(triangle).buffer());
    List<cl_mem> perSegmentBoundaryCountBuffers = ImmutableList.of(session.createIntBuffer(3).buffer());
    List<cl_mem> boundaryPointCountPrefixSumBuffers = ImmutableList.of(session.createIntBuffer(3).buffer());
    cl_mem splittableBuffer = session.createFloatBuffer(1000).buffer();
    SplitBuffer splitBuffer = new SplitBuffer(splittableBuffer, 1000 * Float.BYTES);
    ComputeSolidObjectBoundariesKernel kern = new ComputeSolidObjectBoundariesKernel(session);
    FloatBuffer1D boundaryPoints = kern.compute(
        solidObjectVBOs,
        ImmutableList.of(3),
        perSegmentBoundaryCountBuffers,
        boundaryPointCountPrefixSumBuffers,
        splitBuffer, nx, ny);

//    gridGeometrySimulation = new GridGeometrySimulation(
//        solidObjectVBOs,
//        List.of(boundaryPoints.buffer()),
//
//        )
  }
}
