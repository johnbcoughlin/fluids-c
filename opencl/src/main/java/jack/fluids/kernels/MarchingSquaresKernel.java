package jack.fluids.kernels;

import jack.fluids.buffers.HistogramPyramid;
import jack.fluids.cl.Session;
import org.jocl.*;

import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clSetKernelArg;

public class MarchingSquaresKernel extends AbstractKernel {
  private static final int[] error_code_ret = new int[1];

  private cl_kernel countVerticesKernel;

  protected MarchingSquaresKernel(Session session) {
    super(session);
  }

  public void march(cl_mem phi,
                    HistogramPyramid hp,
                    int width,
                    int height) {
    if (!compiled) {
      throw new RuntimeException("not yet compiled");
    }

    // first populate the bottom level of the histogram pyramid
    clSetKernelArg(countVerticesKernel, 0, Sizeof.cl_mem, Pointer.to(phi));
    clSetKernelArg(countVerticesKernel, 1, Sizeof.cl_mem, Pointer.to(hp.bottom()));
    clEnqueueNDRangeKernel(session.queue(),
        countVerticesKernel, 2,
        new long[] {0, 0, 0},
        new long[] {hp.width(0), hp.height(0), 1},
        new long[] {1, 1, 1},
        0, null, null);

    // roll up the histogram pyramid and get the top value
  }

  @Override
  protected String[] kernelSources() {
    return new String[] {
      kernelSource("marching_squares/count_vertices.cl")
    };
  }

  @Override
  protected void setupKernels() {
    countVerticesKernel = kernel("count_vertices");
  }
}
