package jack.fluids.kernels;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import jack.fluids.CLSimulation;
import jack.fluids.buffers.TwoPhaseBuffer;
import jack.fluids.gl.GLUtils;
import jack.fluids.gl.QuadRender;
import jogamp.opengl.macosx.cgl.CGL;
import org.jocl.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static jack.fluids.cl.JOCLUtils.check;
import static org.jocl.CL.*;

public class SmoothSignedDistanceFunctionKernelTest {
  AtomicReference<GLAutoDrawable> drawable = new AtomicReference<>();

  @Rule public final TestRule rule = new GLWindowRule(drawable);

  GL4 gl;
  QuadRender quadRender;

  cl_context context;
  cl_command_queue queue;
  int[] error_code_ret = new int[1];

  SmoothSignedDistanceFunctionKernel kernel;

  TwoPhaseBuffer phi;

  @Before
  public void before() {
    this.gl = drawable.get().getGL().getGL4();
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
    long shareGroup = CGL.CGLGetShareGroup(CGL.CGLGetCurrentContext());
    contextProperties.addProperty(CLSimulation.CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE, shareGroup);

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
    System.out.println("beginning test");
    int width = 10;
    int height = 10;
    float[] imageData = new float[4 * width * height];
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        if (i == 4) {
          imageData[4 * j * width + 4 * i + 0] = 4.5f;
          imageData[4 * j * width + 4 * i + 1] = 5.0f;
          imageData[4 * j * width + 4 * i + 2] = (float) Math.pow((5.0f - j), 4);
          imageData[4 * j * width + 4 * i + 3] = 1.0f;
        } else if (i == 5) {
          imageData[4 * j * width + 4 * i + 0] = 4.5f;
          imageData[4 * j * width + 4 * i + 1] = 5.0f;
          imageData[4 * j * width + 4 * i + 2] = (float) Math.pow((5.0f - j), 4);
          imageData[4 * j * width + 4 * i + 3] = -1.0f;
        } else {
          imageData[4 * j * width + 4 * i + 0] = 0.0f;
          imageData[4 * j * width + 4 * i + 1] = 0.0f;
          imageData[4 * j * width + 4 * i + 2] = 1000.0f;
          imageData[4 * j * width + 4 * i + 3] = 0.0f;
        }
      }
    }


    int texture1 = GLUtils.createTextureWithData4f(gl, width, height, FloatBuffer.wrap(imageData));
    int texture2 = GLUtils.createTextureWithData4f(gl, width, height, FloatBuffer.wrap(imageData));
    GLUtils.check(gl);
    quadRender = new QuadRender(drawable.get(), texture1);
    quadRender.setup();
    quadRender.draw();

    cl_mem image1 = clCreateFromGLTexture(context, CL_MEM_READ_WRITE, GL4.GL_TEXTURE_2D, 0, texture1, error_code_ret);
    check(error_code_ret);
    cl_mem image2 = clCreateFromGLTexture(context, CL_MEM_READ_WRITE, GL4.GL_TEXTURE_2D, 0, texture2, error_code_ret);
    check(error_code_ret);
    cl_event acquire = new cl_event();
    clEnqueueAcquireGLObjects(queue, 2, new cl_mem[] {image1, image2}, 0, null, acquire);

    clFinish(queue);
    TwoPhaseBuffer twoPhaseBuffer = new TwoPhaseBuffer(image1, image2);
    kernel.smooth(twoPhaseBuffer, width, height, new cl_event[] {acquire});

    clFinish(queue);


    FloatBuffer buf = FloatBuffer.allocate(4 * width * height);
    cl_event readEvent = new cl_event();
    clEnqueueReadImage(queue, image2, CL_TRUE, new long[]{0, 0, 0}, new long[]{width, height, 1}, width * 4 * 4, 0,
        Pointer.to(buf), 0, null, readEvent);
    clFinish(queue);
    System.out.println(Arrays.toString(buf.array()));
    clEnqueueReleaseGLObjects(queue, 2, new cl_mem[] {image1, image2}, 0, null, null);
    clFinish(queue);

    System.out.println("drawing number 2");
    QuadRender quadRender2 = new QuadRender(drawable.get(), texture2);
    quadRender2.setup();
    quadRender2.draw();
  }
}
