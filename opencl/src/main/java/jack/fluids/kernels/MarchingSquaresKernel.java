package jack.fluids.kernels;

import jack.fluids.buffers.HistogramPyramid;
import jack.fluids.cl.Session;
import org.jocl.*;

import static org.jocl.CL.*;

public class MarchingSquaresKernel extends AbstractKernel {
  private cl_kernel countVerticesKernel;
  private cl_kernel rollUpHistogramKernel;

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
        new long[]{0, 0, 0},
        new long[]{hp.width(0), hp.height(0), 1},
        new long[]{1, 1, 1},
        0, null, null);

    cl_event previousEvent = null;
    // roll up the histogram pyramid and get the top value
    for (int level = 0; level < hp.levelCount() - 2; level++) {
      clSetKernelArg(rollUpHistogramKernel, 0, Sizeof.cl_mem, Pointer.to(hp.level(level)));
      clSetKernelArg(rollUpHistogramKernel, 1, Sizeof.cl_mem, Pointer.to(hp.level(level + 1)));
      clSetKernelArg(rollUpHistogramKernel, 2, Sizeof.cl_int, Pointer.to(new int[] {hp.width(level)}));
      clSetKernelArg(rollUpHistogramKernel, 3, Sizeof.cl_int, Pointer.to(new int[] {hp.height(level)}));
      cl_event event = new cl_event();
      clEnqueueNDRangeKernel(session.queue(),
          rollUpHistogramKernel, 2,
          new long[]{0, 0, 0},
          new long[] {hp.width(level + 1), hp.height(level + 1), 1},
          new long[] {1, 1, 1},
          previousEvent == null ? 0 : 1,
          previousEvent == null ? null : new cl_event[] {previousEvent},
          event);
      clFinish(session.queue());
      previousEvent = event;
    }
    clSetKernelArg(rollUpHistogramKernel, 0, Sizeof.cl_mem, Pointer.to(hp.level(3)));
    clSetKernelArg(rollUpHistogramKernel, 1, Sizeof.cl_mem, Pointer.to(hp.level(4)));
    clSetKernelArg(rollUpHistogramKernel, 2, Sizeof.cl_int, Pointer.to(new int[] {hp.width(3)}));
    clSetKernelArg(rollUpHistogramKernel, 3, Sizeof.cl_int, Pointer.to(new int[] {hp.height(3)}));
    cl_event event = new cl_event();
    clEnqueueNDRangeKernel(session.queue(),
        rollUpHistogramKernel, 2,
        new long[]{0, 0, 0},
        new long[] {hp.width(4), hp.height(4), 1},
        new long[] {1, 1, 1},
        previousEvent == null ? 0 : 1,
        previousEvent == null ? null : new cl_event[] {previousEvent},
        event);
    clFinish(session.queue());
  }

  @Override
  protected String[] kernelSources() {
    return new String[]{
        kernelSource("marching_squares/count_vertices.cl"),
        kernelSource("marching_squares/roll_up_histogram.cl")
    };
  }

  @Override
  protected void setupKernels() {
    countVerticesKernel = kernel("count_vertices");
    rollUpHistogramKernel = kernel("roll_up_histogram");
  }
}
