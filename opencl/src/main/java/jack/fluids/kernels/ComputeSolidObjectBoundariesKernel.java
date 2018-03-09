package jack.fluids.kernels;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import jack.fluids.JOCLUtils;
import jack.fluids.buffers.SplitBuffer;
import org.jocl.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static jack.fluids.JOCLUtils.check;
import static org.jocl.CL.*;

public class ComputeSolidObjectBoundariesKernel {
  private static final int[] error_code_ret = new int[1];

  private final cl_context context;
  private final cl_command_queue queue;
  private final cl_device_id[] devices;

  private cl_program program;
  private cl_kernel countBoundaryPointsKernel;
  private cl_kernel prefixSumKernel;

  private boolean compiled;

  public ComputeSolidObjectBoundariesKernel(
      cl_context context,
      cl_command_queue queue,
      cl_device_id[] devices
  ) {
    this.context = context;
    this.queue = queue;
    this.devices = devices;
  }

  public void compile() {
    String countPoints = kernel("count_boundaries.cl");
    String prefixSum = kernel("serial_prefix_sum.cl");
    program = clCreateProgramWithSource(context, 2,
        new String[] {countPoints, prefixSum},
        new long[] {countPoints.length(), prefixSum.length()},
        error_code_ret);
    buildProgramSafely();
    countBoundaryPointsKernel = clCreateKernel(program, "count_boundary_points", error_code_ret);
    check(error_code_ret);
    prefixSumKernel = clCreateKernel(program, "serial_prefix_sum", error_code_ret);
    check(error_code_ret);
    check(error_code_ret);
    this.compiled = true;
  }

  private void buildProgramSafely() {
    int result = clBuildProgram(program, devices.length, devices, null, null, null);
    if (result == CL_BUILD_PROGRAM_FAILURE) {
      throw new RuntimeException(JOCLUtils.obtainBuildLogs(program));
    }
    check(error_code_ret);
  }

  public float[] compute(
      List<cl_mem> solidObjectVertexBuffers,
      List<Integer> segmentCounts,
      List<cl_mem> perSegmentBoundaryCountBuffers,
      List<cl_mem> boundaryPointCountPrefixSumBuffers,
      SplitBuffer boundaryPointVerticesBuffer,
      int invMeshSize
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
    clSetKernelArg(countBoundaryPointsKernel, 0, Sizeof.cl_uint, Pointer.to(new int[] {invMeshSize}));
    clSetKernelArg(countBoundaryPointsKernel, 1, Sizeof.cl_mem, Pointer.to(vertexBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 2, Sizeof.cl_mem, Pointer.to(pointCountBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 3, Sizeof.cl_mem, null);
    clSetKernelArg(countBoundaryPointsKernel, 4, Sizeof.cl_mem, null);
    clSetKernelArg(countBoundaryPointsKernel, 5, Sizeof.cl_int, Pointer.to(new int[] {0}));

    cl_event kernel_event = new cl_event();
    clEnqueueNDRangeKernel(queue, countBoundaryPointsKernel, 1, null,
        new long[]{segmentCount}, new long[]{1}, 0, null, kernel_event);

    IntBuffer perSegmentCounts = IntBuffer.allocate(segmentCount);
    clEnqueueReadBuffer(queue, pointCountBuffer,
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
    clEnqueueNDRangeKernel(queue, prefixSumKernel, 1, null,
        new long[]{1}, new long[]{1}, 0, null, kernel_event);

    IntBuffer prefixSums = IntBuffer.allocate(segmentCount);
    clEnqueueReadBuffer(queue, pointCountPrefixSumBuffer,
        CL_TRUE, 0, segmentCount * Sizeof.cl_uint, Pointer.to(prefixSums), 0, null, null);
    int totalPointCount = prefixSums.array()[segmentCount - 1];
    System.out.println(totalPointCount);

    // finally, write boundary points
    cl_mem boundaryPointBuffer = boundaryPointVerticesBuffer.requestSubBuffer(
        Sizeof.cl_float * 2 * totalPointCount, CL_MEM_READ_WRITE);
    clSetKernelArg(countBoundaryPointsKernel, 0, Sizeof.cl_uint, Pointer.to(new int[] {invMeshSize}));
    clSetKernelArg(countBoundaryPointsKernel, 1, Sizeof.cl_mem, Pointer.to(vertexBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 2, Sizeof.cl_mem, null);
    clSetKernelArg(countBoundaryPointsKernel, 3, Sizeof.cl_mem, Pointer.to(pointCountPrefixSumBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 4, Sizeof.cl_mem, Pointer.to(boundaryPointBuffer));
    clSetKernelArg(countBoundaryPointsKernel, 5, Sizeof.cl_int, Pointer.to(new int[] {1}));

    kernel_event = new cl_event();
    clEnqueueNDRangeKernel(queue, countBoundaryPointsKernel, 1, null,
        new long[]{segmentCount}, new long[]{1}, 0, null, kernel_event);
    clFinish(queue);

    FloatBuffer result = FloatBuffer.allocate(2 * totalPointCount);
    clEnqueueReadBuffer(queue, boundaryPointBuffer,
        CL_TRUE, 0, totalPointCount * 2 * Sizeof.cl_float, Pointer.to(result), 0, null, null);
    System.out.println(Arrays.toString(result.array()));

    System.out.println(Arrays.toString(prefixSums.array()));
    return result.array();
  }

  String kernel(String fileName) {
    try {
      return CharStreams.toString(new InputStreamReader(
          getClass().getResourceAsStream("/kernels/" + fileName)));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
