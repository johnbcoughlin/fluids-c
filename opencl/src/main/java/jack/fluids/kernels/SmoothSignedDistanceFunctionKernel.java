package jack.fluids.kernels;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import jack.fluids.JOCLUtils;
import jack.fluids.buffers.TwoPhaseBuffer;
import org.jocl.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static jack.fluids.JOCLUtils.check;
import static org.jocl.CL.*;

public class SmoothSignedDistanceFunctionKernel {
  private static final int[] error_code_ret = new int[1];

  private final cl_context context;
  private final cl_command_queue queue;
  private final cl_device_id[] devices;

  private cl_program program;
  private cl_kernel kernel;

  private boolean compiled;

  public SmoothSignedDistanceFunctionKernel(
      cl_context context,
      cl_command_queue queue,
      cl_device_id[] devices
  ) {
    this.context = context;
    this.queue = queue;
    this.devices = devices;
  }

  public void compile() {
    String src = kernel("eikonal.cl");
    program = clCreateProgramWithSource(context, 1,
        new String[]{src},
        new long[]{src.length()},
        error_code_ret);
    buildProgramSafely();
    kernel = clCreateKernel(program, "iterate_eikonal", error_code_ret);
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

  public void smooth(
      TwoPhaseBuffer twoPhaseBuffer,
      int nx, int ny,
      cl_event[] waitList) {
    if (!compiled) {
      throw new RuntimeException("not compiled yet");
    }
    List<Integer> dxs = ImmutableList.of(
        1, 1, 1, -1, -1, -1, 0, 0, 0, 0, 0, 0);

    List<Integer> dys = ImmutableList.of(
        0, 0, 0, 0, 0, 0, 1, 1, 1, -1, -1, -1);

    for (int k = 0; k < 2; k++) {
      for (int i = 0; i < dxs.size(); i++) {
        int dx = dxs.get(i);
        int dy = dys.get(i);
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(twoPhaseBuffer.front()));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(twoPhaseBuffer.back()));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{dx}));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{dy}));
        cl_event event = new cl_event();
        clEnqueueNDRangeKernel(queue, kernel, 2,
            new long[]{0, 0, 0},
            new long[]{10, 10, 1},
            new long[]{2, 2, 1},
            0, null, event);
        twoPhaseBuffer.swap();
      }
    }
    clFinish(queue);
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
