package jack.fluids.kernels;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import jack.fluids.buffers.TwoPhaseBuffer;
import org.jocl.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static jack.fluids.JOCLUtils.check;
import static org.jocl.CL.*;

public class SmoothSignedDistanceFunctionKernelTest implements GLEventListener {
  cl_context context;
  cl_command_queue queue;
  int[] error_code_ret = new int[1];

  SmoothSignedDistanceFunctionKernel kernel;

  TwoPhaseBuffer phi;

  @Before
  public void before() {
    GLProfile profile = GLProfile.get(GLProfile.GL4);
    GLCapabilities capabilities = new GLCapabilities(profile);
    GLWindow window = GLWindow.create(capabilities);
    window.setTitle("a triangle");
    window.setSize(200, 200);
    window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
    window.setVisible(true);



    int numPlatformsArray[] = new int[1];
    clGetPlatformIDs(0, null, numPlatformsArray);
    int numPlatforms = numPlatformsArray[0];

    cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
    clGetPlatformIDs(numPlatforms, platforms, null);

    cl_platform_id platform = platforms[0];

    long deviceType = CL_DEVICE_TYPE_GPU;
    int numDevicesArray[] = new int[1];

    clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
    int numDevices = numDevicesArray[0];

    cl_device_id devices[] = new cl_device_id[numDevices];
    clGetDeviceIDs(platform, deviceType, numDevices, devices, null);

    cl_context_properties contextProperties = new cl_context_properties();
    contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

    context = clCreateContext(contextProperties, numDevices, devices, new CreateContextFunction() {
      public void function(String s, Pointer pointer, long l, Object o) {
      }
    }, null, error_code_ret);
    check(error_code_ret);
    queue = clCreateCommandQueue(context, devices[0],
        CL_QUEUE_PROFILING_ENABLE, error_code_ret);

    kernel = new SmoothSignedDistanceFunctionKernel(context, queue, devices);
    kernel.compile();
  }

  @After
  public void after() {
    clReleaseCommandQueue(queue);
    clReleaseContext(context);
  }

  @Test
  public void test() {

  }

  @Override
  public void init(GLAutoDrawable drawable) {

  }

  @Override
  public void dispose(GLAutoDrawable drawable) {

  }

  @Override
  public void display(GLAutoDrawable drawable) {

  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

  }
}
