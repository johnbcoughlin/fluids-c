package jack.fluids.kernels;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import jack.fluids.cl.JOCLUtils;
import jack.fluids.cl.Session;
import org.jocl.cl_kernel;
import org.jocl.cl_program;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Stream;

import static jack.fluids.cl.JOCLUtils.check;
import static org.jocl.CL.*;

public abstract class AbstractKernel {
  private static final int[] error_code_ret = new int[1];

  protected final Session session;

  private cl_program program;

  protected boolean compiled;

  protected AbstractKernel(Session session) {
    this.session = session;
  }

  protected abstract String[] kernelSources();

  public void compile() {
    String[] sources = kernelSources();
    program = clCreateProgramWithSource(session.context(),
        sources.length,
        sources,
        Stream.of(sources).mapToLong(String::length).toArray(),
        error_code_ret);

    int buildResult = clBuildProgram(program, session.device_ids().length,
        session.device_ids(), null, null, null);
    if (buildResult == CL_BUILD_PROGRAM_FAILURE) {
      throw new RuntimeException(JOCLUtils.obtainBuildLogs(program));
    }
    check(error_code_ret);

    setupKernels();

    this.compiled = true;
  }

  protected abstract void setupKernels();

  protected cl_kernel kernel(String functionName) {
    cl_kernel result = clCreateKernel(program, functionName, error_code_ret);
    check(error_code_ret);
    return result;
  }

  public static String kernelSource(String fileName) {
    try {
      return CharStreams.toString(new InputStreamReader(
          AbstractKernel.class.getResourceAsStream("/kernels/" + fileName)));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
