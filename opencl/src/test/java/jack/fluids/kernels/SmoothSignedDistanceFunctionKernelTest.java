package jack.fluids.kernels;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import jack.fluids.CLSimulation;
import jack.fluids.buffers.TwoPhaseBuffer;
import jack.fluids.glutils.GLUtils;
import jack.fluids.glutils.QuadRender;
import jogamp.opengl.macosx.cgl.CGL;
import org.jocl.*;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.nio.FloatBuffer;
import java.util.concurrent.CompletableFuture;

import static jack.fluids.JOCLUtils.check;
import static org.jocl.CL.*;

public class SmoothSignedDistanceFunctionKernelTest implements GLEventListener {
  CompletableFuture<Void> initializationFuture = new CompletableFuture<>();
  CompletableFuture<Void> testCompletionFuture = new CompletableFuture<>();

  @Rule public final TestRule rule = new GLWindowRule(this, initializationFuture, testCompletionFuture);

  GL4 gl;
  QuadRender quadRender;

  cl_context context;
  cl_command_queue queue;
  int[] error_code_ret = new int[1];

  SmoothSignedDistanceFunctionKernel kernel;

  TwoPhaseBuffer phi;

  public void before(GL4 gl) {
    this.gl = gl;
    System.out.println("in before");
    System.out.println(gl);
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
    initializationFuture.complete(null);
  }

  @After
  public void after() {
    clReleaseCommandQueue(queue);
    clReleaseContext(context);
    testCompletionFuture.complete(null);
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
          imageData[4 * j * width + 4 * i + 1] = j;
          imageData[4 * j * width + 4 * i + 2] = -0.5f;
        } else if (i == 5) {
          imageData[4 * j * width + 4 * i + 0] = 4.5f;
          imageData[4 * j * width + 4 * i + 1] = j;
          imageData[4 * j * width + 4 * i + 2] = 0.5f;
        } else {
          imageData[4 * j * width + 4 * i + 2] = 100.0f;
        }
        imageData[4 * j * width + 4 * i + 3] = 1.0f;
      }
    }

    int texture = GLUtils.createTexture(gl);
    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL4.GL_R32F, width, height, 0, gl.GL_RGBA, gl.GL_FLOAT,
        FloatBuffer.wrap(imageData));
    quadRender = new QuadRender(gl, texture);
    quadRender.setup();

    cl_image_format imageFormat = new cl_image_format();
    imageFormat.image_channel_order = CL_RGBA;
    imageFormat.image_channel_data_type = CL_FLOAT;

    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        System.out.print(String.format(
            "(%.1f, %.1f, %.1f, %.1f), ",
            imageData[4 * j * width + 4 * i + 0],
            imageData[4 * j * width + 4 * i + 1],
            imageData[4 * j * width + 4 * i + 2],
            imageData[4 * j * width + 4 * i + 3]
        ));
      }
      System.out.println();
    }

    cl_mem inputImageMem = clCreateImage2D(
        context, CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR,
        new cl_image_format[]{imageFormat}, width, height,
        width * 4 * Sizeof.cl_float, Pointer.to(imageData), error_code_ret);
    check(error_code_ret);
  }

  @Override
  public void init(GLAutoDrawable drawable) {
    this.before(drawable.getGL().getGL4());
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {

  }

  @Override
  public void display(GLAutoDrawable drawable) {
//    System.out.println("displyaing");
    if (quadRender != null) {
      quadRender.draw();
    }
    drawable.swapBuffers();
  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

  }
}
