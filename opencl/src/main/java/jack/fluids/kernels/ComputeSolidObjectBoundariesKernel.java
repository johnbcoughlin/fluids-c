package jack.fluids.kernels;

import jack.fluids.buffers.FloatBuffer1D;
import jack.fluids.buffers.SplitBuffer;
import jack.fluids.cl.Session;
import org.jocl.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.IntStream;

import static jack.fluids.cl.JOCLUtils.check;
import static org.jocl.CL.*;

public class ComputeSolidObjectBoundariesKernel extends AbstractKernel {
  private static final int[] error_code_ret = new int[1];

  private cl_kernel countBoundaryPointsKernel;
  private cl_kernel prefixSumKernel;

  public ComputeSolidObjectBoundariesKernel(Session session) {
    super(session);
  }

  @Override
  protected String[] kernelSources() {
    return new String[] {
        kernelSource("solid_objects/count_boundaries.cl"),
        kernelSource("solid_objects/serial_prefix_sum.cl")
    };
  }

  @Override
  protected void setupKernels() {
    countBoundaryPointsKernel = kernel("count_boundary_points");
    prefixSumKernel = kernel("serial_prefix_sum");
  }

  public FloatBuffer1D compute(
      List<cl_mem> solidObjectVertexBuffers,
      List<Integer> segmentCounts,
      List<cl_mem> perSegmentBoundaryCountBuffers,
      List<cl_mem> boundaryPointCountPrefixSumBuffers,
      SplitBuffer boundaryPointVerticesBuffer,
      int width,
      int height
  ) {
    if (!compiled) {
      throw new RuntimeException("not compiled yet");
    }
    // first count the boundary points on each segment
    int objectIndex = 0;
    cl_mem vertexBuffer = solidObjectVertexBuffers.get(objectIndex);
    Integer segmentCount = segmentCounts.get(objectIndex);
    cl_mem pointCountBuffer = perSegmentBoundaryCountBuffers.get(objectIndex);

    check(error_code_ret);
    clSetKernelArg(countBoundaryPointsKernel, 0, Sizeof.cl_uint, Pointer.to(new int[] {width}));
    clSetKernelArg(countBoundaryPointsKernel, 1, Sizeof.cl_uint, Pointer.to(new int[] {height}));
    clSetKernelArg(countBoundaryPointsKernel, 2, Sizeof.cl_mem, Pointer.to(vertexBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 3, Sizeof.cl_mem, Pointer.to(pointCountBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 4, Sizeof.cl_mem, null);
    clSetKernelArg(countBoundaryPointsKernel, 5, Sizeof.cl_mem, null);
    clSetKernelArg(countBoundaryPointsKernel, 6, Sizeof.cl_int, Pointer.to(new int[] {0}));

    cl_event kernel_event = new cl_event();
    clEnqueueNDRangeKernel(session.queue(), countBoundaryPointsKernel, 1, null,
        new long[]{segmentCount}, new long[]{1}, 0, null, kernel_event);

    IntBuffer perSegmentCounts = IntBuffer.allocate(segmentCount);
    clEnqueueReadBuffer(session.queue(), pointCountBuffer,
        CL_TRUE, 0, segmentCount * Sizeof.cl_uint, Pointer.to(perSegmentCounts), 0, null, null);
    if (IntStream.of(perSegmentCounts.array()).anyMatch(c -> c < 0)) {
      throw new IllegalArgumentException("too many intersections in one segment");
    }

    // now do a (serial) prefix sum to compute the write offsets of boundary points for each segment.
    cl_mem pointCountPrefixSumBuffer = boundaryPointCountPrefixSumBuffers.get(objectIndex);
    clSetKernelArg(prefixSumKernel, 0, Sizeof.cl_mem, Pointer.to(pointCountBuffer));
    clSetKernelArg(prefixSumKernel, 1, Sizeof.cl_mem, Pointer.to(pointCountPrefixSumBuffer));
    clSetKernelArg(prefixSumKernel, 2, Sizeof.cl_uint, Pointer.to(new int[] {segmentCount}));

    kernel_event = new cl_event();
    clEnqueueNDRangeKernel(session.queue(), prefixSumKernel, 1, null,
        new long[]{1}, new long[]{1}, 0, null, kernel_event);

    IntBuffer prefixSums = IntBuffer.allocate(segmentCount);
    clEnqueueReadBuffer(session.queue(), pointCountPrefixSumBuffer,
        CL_TRUE, 0, segmentCount * Sizeof.cl_uint, Pointer.to(prefixSums), 0, null, null);
    int totalPointCount = prefixSums.array()[segmentCount - 1];

    // finally, write boundary points
    cl_mem boundaryPointBuffer = boundaryPointVerticesBuffer.requestSubBuffer(
        Sizeof.cl_float * 2 * totalPointCount, CL_MEM_READ_WRITE);
    clSetKernelArg(countBoundaryPointsKernel, 0, Sizeof.cl_uint, Pointer.to(new int[] {width}));
    clSetKernelArg(countBoundaryPointsKernel, 1, Sizeof.cl_uint, Pointer.to(new int[] {height}));
    clSetKernelArg(countBoundaryPointsKernel, 2, Sizeof.cl_mem, Pointer.to(vertexBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 3, Sizeof.cl_mem, null);
    clSetKernelArg(countBoundaryPointsKernel, 4, Sizeof.cl_mem, Pointer.to(pointCountPrefixSumBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 5, Sizeof.cl_mem, Pointer.to(boundaryPointBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 6, Sizeof.cl_int, Pointer.to(new int[] {1}));

    kernel_event = new cl_event();
    clEnqueueNDRangeKernel(session.queue(), countBoundaryPointsKernel, 1, null,
        new long[]{segmentCount}, new long[]{1}, 0, null, kernel_event);
    clFinish(session.queue());

    FloatBuffer result = FloatBuffer.allocate(2 * totalPointCount);
    clEnqueueReadBuffer(session.queue(), boundaryPointBuffer,
        CL_TRUE, 0, totalPointCount * 2 * Sizeof.cl_float, Pointer.to(result), 0, null, null);

    return FloatBuffer1D.of(boundaryPointBuffer, totalPointCount * 2);
  }
}
