package jack.fluids.kernels;

import com.jogamp.opengl.GL4;
import jack.fluids.buffers.HistogramPyramid;
import jack.fluids.buffers.SharedVBO;
import jack.fluids.cl.Session;
import jack.fluids.clgl.CLGLUtils;
import org.jocl.*;

import static jack.fluids.cl.JOCLUtils.check;
import static org.jocl.CL.*;

public class MarchingSquaresKernel extends AbstractKernel {
  private cl_kernel countSegmentsKernel;
  private cl_kernel rollUpHistogramKernel;
  private cl_kernel generateSegmentsKernel;

  private final GL4 gl;

  protected MarchingSquaresKernel(Session session, GL4 gl) {
    super(session);
    this.gl = gl;
  }

  public SharedVBO march(cl_mem phi,
                         HistogramPyramid hp,
                         int width,
                         int height) {
    if (!compiled) {
      throw new RuntimeException("not yet compiled");
    }

    // first populate the bottom level of the histogram pyramid
    clSetKernelArg(countSegmentsKernel, 0, Sizeof.cl_mem, Pointer.to(phi));
    clSetKernelArg(countSegmentsKernel, 1, Sizeof.cl_mem, Pointer.to(hp.bottom()));
    clEnqueueNDRangeKernel(session.queue(),
        countSegmentsKernel, 2,
        new long[]{0, 0, 0},
        new long[]{hp.width(0), hp.height(0), 1},
        new long[]{1, 1, 1},
        0, null, null);

    cl_event previousEvent = null;
    // roll up the histogram pyramid and get the top value
    for (int level = 0; level < hp.levelCount() - 1; level++) {
      clSetKernelArg(rollUpHistogramKernel, 0, Sizeof.cl_mem, Pointer.to(hp.level(level)));
      clSetKernelArg(rollUpHistogramKernel, 1, Sizeof.cl_mem, Pointer.to(hp.level(level + 1)));
      clSetKernelArg(rollUpHistogramKernel, 2, Sizeof.cl_int, Pointer.to(new int[]{hp.width(level)}));
      clSetKernelArg(rollUpHistogramKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{hp.height(level)}));
      cl_event event = new cl_event();
      clEnqueueNDRangeKernel(session.queue(),
          rollUpHistogramKernel, 2,
          new long[]{0, 0, 0},
          new long[]{hp.width(level + 1), hp.height(level + 1), 1},
          new long[]{1, 1, 1},
          previousEvent == null ? 0 : 1,
          previousEvent == null ? null : new cl_event[]{previousEvent},
          event);
      clFinish(session.queue());
      previousEvent = event;
    }

    int segmentCount = session.readInt2DImage(hp.top(), 1, 1)[0];
    System.out.println(segmentCount);
    SharedVBO vbo = CLGLUtils.createSharedVBO(segmentCount * 4, session, gl);

    clSetKernelArg(generateSegmentsKernel, 0, Sizeof.cl_int, Pointer.to(new int[]{hp.levelCount()}));
    System.out.println(vbo.clBufferHandle());
    clSetKernelArg(generateSegmentsKernel, 1, Sizeof.cl_mem, Pointer.to(vbo.clBufferHandle()));
    clSetKernelArg(generateSegmentsKernel, 2, Sizeof.cl_mem, Pointer.to(phi));
    for (int i = 0; i < hp.levelCount(); i++) {
      check(clSetKernelArg(generateSegmentsKernel, i + 3, Sizeof.cl_mem, Pointer.to(hp.level(i))));
    }
    check(clEnqueueNDRangeKernel(session.queue(),
        generateSegmentsKernel, 1,
        new long[]{0, 0, 0},
        new long[]{segmentCount, 1, 1},
        new long[]{1, 1, 1},
        0, null, null));
    clFinish(session.queue());

    return vbo;
  }

  @Override
  protected String[] kernelSources() {
    return new String[]{
        kernelSource("marching_squares/lookup.cl"),
        kernelSource("marching_squares/count_segments.cl"),
        kernelSource("marching_squares/roll_up_histogram.cl"),
        kernelSource("marching_squares/generate_segments.cl")
    };
  }

  @Override
  protected void setupKernels() {
    countSegmentsKernel = kernel("count_segments");
    rollUpHistogramKernel = kernel("roll_up_histogram");
    generateSegmentsKernel = kernel("generate_segments__5");
  }
}
