package jack.fluids;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import jack.fluids.glutils.GLUtils;
import jogamp.opengl.macosx.cgl.CGL;
import org.jocl.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static jack.fluids.CLSimulation.CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE;
import static jack.fluids.JOCLUtils.check;
import static org.jocl.CL.*;

public class TestMain implements GLEventListener {
  private static final int[] error_code_ret = new int[1];

  public static void main(String[] args) {
    GLProfile profile = GLProfile.get(GLProfile.GL4);
    GLCapabilities capabilities = new GLCapabilities(profile);
    GLWindow window = GLWindow.create(capabilities);
    window.setTitle("a triangle");
    window.setSize(200, 200);
    window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
    window.setVisible(true);

    TestMain test = new TestMain();
    window.addGLEventListener(test);
    final Animator animator = new Animator(window);
    animator.start();

    window.addKeyListener(new KeyListener() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyChar() == 'w') {
          animator.stop();
          System.exit(0);
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {

      }
    });

    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowDestroyed(WindowEvent e) {
        animator.stop();
        System.exit(0);
      }
    });
  }

  @Override
  public void init(GLAutoDrawable drawable) {
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

    long deviceType = CL_DEVICE_TYPE_GPU;
    int gpuDeviceIndex = 0;

    int numDevicesArray[] = new int[1];
    clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
    int numDevices = numDevicesArray[0];
    System.out.println(numDevices);

    cl_device_id devices[] = new cl_device_id[numDevices];
    clGetDeviceIDs(platform, deviceType, numDevices, devices, null);

    int error_code_ret[] = new int[1];

    cl_context context = clCreateContext(contextProperties, numDevices, devices, new CreateContextFunction() {
      public void function(String s, Pointer pointer, long l, Object o) {
      }
    }, null, error_code_ret);

    cl_command_queue queue = clCreateCommandQueue(context, devices[gpuDeviceIndex],
        CL_QUEUE_PROFILING_ENABLE, error_code_ret);

    cl_image_format image_format = new cl_image_format();
    image_format.image_channel_order = CL_RGBA;
    image_format.image_channel_data_type = CL_FLOAT;
    cl_image_desc image_desc = new cl_image_desc();

    float[] imageData = new float[400];

    cl_mem image1 = clCreateImage2D(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
        new cl_image_format[] {image_format},
        10, 10, 4 * Sizeof.cl_float * 100, Pointer.to(imageData), error_code_ret);

    int texture1 = GLUtils.createTextureWithData(drawable.getGL().getGL4(), 10, 10, FloatBuffer.allocate(400));
    int texture2 = GLUtils.createTextureWithData(drawable.getGL().getGL4(), 10, 10, FloatBuffer.allocate(400));

    cl_mem image2 = clCreateFromGLTexture(context, CL_MEM_READ_WRITE, GL4.GL_TEXTURE_2D, 0, texture1, error_code_ret);
    check(error_code_ret);
    cl_mem image3 = clCreateFromGLTexture(context, CL_MEM_READ_WRITE, GL4.GL_TEXTURE_2D, 0, texture2, error_code_ret);
    check(error_code_ret);

    clEnqueueAcquireGLObjects(queue, 1, new cl_mem[]{image2}, 0, null, null);
    clEnqueueAcquireGLObjects(queue, 1, new cl_mem[]{image3}, 0, null, null);


    String src = kernel();
    System.out.println(src);
    cl_program program = clCreateProgramWithSource(context, 1,
        new String[] {src},
        new long[] {src.length()},
        error_code_ret);
    int result = clBuildProgram(program, devices.length, devices, null, null, null);
    if (result == CL_BUILD_PROGRAM_FAILURE) {
      throw new RuntimeException(JOCLUtils.obtainBuildLogs(program));
    }
    check(error_code_ret);
    cl_kernel kernel = clCreateKernel(program, "test", error_code_ret);
    check(error_code_ret);

    clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(image3));
    clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(image2));
    cl_event event = new cl_event();
    clEnqueueNDRangeKernel(queue, kernel, 2,
        new long[] {0, 0, 0},
        new long[] {10, 10, 1},
        new long[] {1, 1, 1},
        0, null, event);
    clFinish(queue);

    FloatBuffer buf = FloatBuffer.allocate(4 * 10 * 10);
    cl_event readEvent = new cl_event();
    clEnqueueReadImage(queue, image2, CL_TRUE, new long[]{0, 0, 0}, new long[]{10, 10, 1}, 10 * 4 * 4, 0,
        Pointer.to(buf), 0, null, readEvent);
    clFinish(queue);
    System.out.println(Arrays.toString(buf.array()));
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

  static String kernel() {
    try {
      return CharStreams.toString(new InputStreamReader(FluidsMain.class.getResourceAsStream("/kernels/test.cl")));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
