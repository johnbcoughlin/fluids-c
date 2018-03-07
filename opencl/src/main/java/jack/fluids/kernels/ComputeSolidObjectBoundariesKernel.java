package jack.fluids.kernels;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import org.jocl.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import static jack.fluids.JOCLUtils.check;
import static org.jocl.CL.*;

public class ComputeSolidObjectBoundariesKernel {
  private static final int[] error_code_ret = new int[1];

  private final List<cl_mem> solidObjectVertexBuffers;
  private final List<Integer> vertexCounts;

  private final cl_context context;
  private final cl_command_queue queue;
  private final cl_device_id[] devices;
  private final int nx;
  private final int ny;

  private cl_program program;
  private cl_kernel countBoundaryPointsKernel;

  public ComputeSolidObjectBoundariesKernel(
      List<cl_mem> solidObjectVertexBuffers,
      List<Integer> vertexCounts,
      cl_context context,
      cl_command_queue queue,
      cl_device_id[] devices,
      int nx,
      int ny
  ) {
    this.solidObjectVertexBuffers = solidObjectVertexBuffers;
    this.vertexCounts = vertexCounts;
    this.context = context;
    this.queue = queue;
    this.devices = devices;
    this.nx = nx;
    this.ny = ny;
  }

  public void compile() {
    String kernelSrc = kernel();
    program = clCreateProgramWithSource(context, 1,
        new String[] {kernelSrc}, new long[] {(long) kernelSrc.length()}, error_code_ret);
    check(error_code_ret);
    clBuildProgram(program, devices.length, devices, null, null, null);
    countBoundaryPointsKernel = clCreateKernel(program, "count_boundary_points", error_code_ret);
    check(error_code_ret);
  }

  public void compute() {
    int objectIndex = 0;
    clSetKernelArg(countBoundaryPointsKernel, 0, Sizeof.cl_uint, Pointer.to(new int[] {128}));
    clSetKernelArg(countBoundaryPointsKernel, 1, Sizeof.cl_mem,
        Pointer.to(solidObjectVertexBuffers.get(objectIndex)));
    Integer vertexCount = vertexCounts.get(objectIndex);
    clSetKernelArg(countBoundaryPointsKernel, 2, Sizeof.cl_uint,
        Pointer.to(new int[] {vertexCount}));

    IntBuffer perSegmentBoundaryPointCounts = IntBuffer.allocate(vertexCount);
    cl_mem countOutput = clCreateBuffer(context, CL_MEM_READ_WRITE, vertexCount,
        Pointer.to(perSegmentBoundaryPointCounts), error_code_ret);
    check(error_code_ret);
    clSetKernelArg(countBoundaryPointsKernel, 3, Sizeof.cl_mem, Pointer.to(countOutput));

    cl_event kernel_event = new cl_event();
    clEnqueueNDRangeKernel(queue, countBoundaryPointsKernel, 1, null,
        new long[]{vertexCount}, new long[]{1}, 0, null, kernel_event);
    IntBuffer result = IntBuffer.allocate(vertexCount);
    clEnqueueReadBuffer(queue, countOutput, CL_TRUE, 0, vertexCount * Sizeof.cl_uint,
        Pointer.to(result), 0, null, null);
    System.out.println(Arrays.toString(result.array()));
  }

  String kernel() {
    try {
      return CharStreams.toString(new InputStreamReader(
          getClass().getResourceAsStream("/kernels/boundaries.cl")));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
