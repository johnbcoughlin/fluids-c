package jack.fluids;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.jogamp.opengl.GL4;
import jogamp.opengl.macosx.cgl.CGL;
import org.jocl.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static org.jocl.CL.*;

public class CLSimulation {
  private static final long CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE = 0x10000000L;

  private final GL4 gl;
  private final int nx;
  private final int ny;

  private cl_context context;
  private cl_command_queue queue;

  public CLSimulation(
      GL4 gl,
      int nx,
      int ny
      ) {
    this.gl = gl;
    this.nx = nx;
    this.ny = ny;
  }

  public void init() {
    int numPlatformsArray[] = new int[1];
    clGetPlatformIDs(0, null, numPlatformsArray);
    int numPlatforms = numPlatformsArray[0];

    cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
    clGetPlatformIDs(numPlatforms, platforms, null);

    cl_platform_id platform = platforms[0];

    cl_context_properties contextProperties = new cl_context_properties();
    contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
    long shareGroup = CGL.CGLGetShareGroup(CGL.CGLGetCurrentContext());
    contextProperties.addProperty(CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE, shareGroup);

    long deviceType = CL_DEVICE_TYPE_ALL;
    int gpuDeviceIndex = 1;

    int numDevicesArray[] = new int[1];
    clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
    int numDevices = numDevicesArray[0];

    cl_device_id devices[] = new cl_device_id[numDevices];
    clGetDeviceIDs(platform, deviceType, numDevices, devices, null);

    int error_code_ret[] = new int[1];

    context = clCreateContext(contextProperties, numDevices, devices, new CreateContextFunction() {
      public void function(String s, Pointer pointer, long l, Object o) {
      }
    }, null, error_code_ret);
    System.out.println(Arrays.toString(error_code_ret));

    queue = clCreateCommandQueue(context, devices[gpuDeviceIndex],
        CL_QUEUE_PROFILING_ENABLE, error_code_ret);

    String kernelSrc = kernel();
    cl_program cl_program = clCreateProgramWithSource(context, 1,
        new String[]{kernelSrc}, new long[]{(long) kernelSrc.length()}, error_code_ret);
    clBuildProgram(cl_program, 2, devices, null, null, null);

    // assemble buffers
    int n = 10;
    float input[] = new float[n];
    float output[] = new float[n];
    for (int i = 0; i < n; i++) {
      input[i] = i;
    }
    Pointer inputPointer = Pointer.to(input);
    Pointer outputPointer = Pointer.to(output);

    cl_mem[] memObjects = new cl_mem[2];
    memObjects[0] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
        Sizeof.cl_float * n, inputPointer, null);
    memObjects[1] = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR,
        Sizeof.cl_float * n, outputPointer, null);

    cl_mem image = clCreateFromGLTexture2D(context, CL_MEM_READ_WRITE, GL4.GL_TEXTURE_2D, 0, 1, error_code_ret);
    System.out.println(image);
    System.out.println(Arrays.toString(error_code_ret));
    cl_event event = new cl_event();
    clEnqueueAcquireGLObjects(queue, 1, new cl_mem[]{image}, 0, null, event);
    clFinish(queue);
    long[] result = new long[1];
    clGetEventInfo(event, CL_EVENT_COMMAND_EXECUTION_STATUS, Sizeof.cl_ulong, Pointer.to(result), null);
    System.out.println(Arrays.toString(result));

    cl_kernel kernel = clCreateKernel(cl_program, "square", null);

    clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
    clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));

    long[] global_work_size = new long[]{n};
    long[] local_work_size = new long[]{1};

    cl_event kernel_event = new cl_event();
    clEnqueueNDRangeKernel(queue, kernel, 1, null, global_work_size, local_work_size, 0, null, kernel_event);
    clEnqueueReadBuffer(queue, memObjects[1], CL_TRUE, 0, n * Sizeof.cl_float, outputPointer, 0, null, null);

    FloatBuffer buf = FloatBuffer.allocate(5000);
    cl_event readEvent = new cl_event();
    clEnqueueReadImage(queue, image, CL_TRUE, new long[]{0, 0, 0}, new long[]{50, 50, 1}, 1, 0, Pointer.to(buf), 0,
        null, readEvent);
    clFinish(queue);
    long[] readResult = new long[1];
    clGetEventInfo(event, CL_EVENT_COMMAND_EXECUTION_STATUS, Sizeof.cl_ulong, Pointer.to(readResult), null);
    System.out.println(Arrays.toString(buf.array()));

    long[] startTime = new long[1];
    long[] endTime = new long[1];
    Pointer startTimePtr = Pointer.to(startTime);
    Pointer endTimePtr = Pointer.to(endTime);
    clGetEventProfilingInfo(kernel_event, CL_PROFILING_COMMAND_START, Sizeof.cl_ulong, startTimePtr, null);
    clGetEventProfilingInfo(kernel_event, CL_PROFILING_COMMAND_END, Sizeof.cl_ulong, endTimePtr, null);
    System.out.println(endTime[0] - startTime[0] + "ns");

    clReleaseMemObject(memObjects[0]);
    clReleaseMemObject(memObjects[1]);
    clReleaseKernel(kernel);
    clReleaseProgram(cl_program);
    clReleaseCommandQueue(queue);
    clReleaseContext(context);

    System.out.println(Arrays.toString(output));
  }

  static String kernel() {
    try {
      return CharStreams.toString(new InputStreamReader(FluidsMain.class.getResourceAsStream("/kernels/square.cl")));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
