package jack.fluids.kernels;

import jack.fluids.buffers.TwoPhaseBuffer;
import jack.fluids.cl.Session;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clFinish;
import static org.jocl.CL.clSetKernelArg;

public class ComputeWindingNumberKernel extends AbstractKernel {
  private cl_kernel computeWindingNumberKernel;

  protected ComputeWindingNumberKernel(Session session) {
    super(session);
  }

  public void computeWindingNumbers(
      List<cl_mem> vertexBuffers,
      List<Integer> segmentCounts,
      List<Integer> strides,
      TwoPhaseBuffer grid,
      float[] origin,
      int width,
      int height
  ) {
    if (!compiled) {
      throw new RuntimeException("not yet compiled");
    }

    int count = vertexBuffers.size();
    checkArgument(segmentCounts.size() == count, "segmentCounts and vertexBuffers sizes must match");
    checkArgument(strides.size() == count, "strides and vertexBuffers sizes must match");
    for (int i = 0; i < count; i++) {
      clSetKernelArg(computeWindingNumberKernel, 0, Sizeof.cl_mem, Pointer.to(vertexBuffers.get(i)));
      clSetKernelArg(computeWindingNumberKernel, 1, Sizeof.cl_int, Pointer.to(new int[] {segmentCounts.get(i)}));
      clSetKernelArg(computeWindingNumberKernel, 2, Sizeof.cl_int, Pointer.to(new int[] {strides.get(i)}));
      clSetKernelArg(computeWindingNumberKernel, 3, Sizeof.cl_mem, Pointer.to(grid.front()));
      clSetKernelArg(computeWindingNumberKernel, 4, Sizeof.cl_mem, Pointer.to(grid.back()));
      clSetKernelArg(computeWindingNumberKernel, 5, Sizeof.cl_float2, Pointer.to(origin));

      clEnqueueNDRangeKernel(session.queue(), computeWindingNumberKernel, 2,
          new long[] {0, 0, 0},
          new long[] {width, height, 1},
          new long[] {1, 1, 1},
          0, null, null);
      clFinish(session.queue());
    }
  }

  @Override
  protected String[] kernelSources() {
    return new String[] {
        kernelSource("winding_number.cl")
    };
  }

  @Override
  protected void setupKernels() {
    computeWindingNumberKernel = kernel("compute_winding_number");
  }
}
