package jack.fluids.cl;

import jack.fluids.CLSimulation;
import jack.fluids.buffers.FloatBuffer1D;
import jack.fluids.buffers.SharedVBO;
import jack.fluids.buffers.SizedBuffer1D;
import jogamp.opengl.macosx.cgl.CGL;
import org.jocl.*;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.stream.Stream;

import static jack.fluids.cl.JOCLUtils.check;
import static org.jocl.CL.*;

public class Session {
  private static final int[] error_code_ret = new int[1];

  private final cl_context context;
  private final cl_command_queue queue;
  private final cl_device_id[] device_ids;

  public Session(cl_context context, cl_command_queue queue, cl_device_id[] device_ids) {
    this.context = context;
    this.queue = queue;
    this.device_ids = device_ids;
  }

  public cl_context context() {
    return context;
  }

  public cl_command_queue queue() {
    return queue;
  }

  public cl_device_id[] device_ids() {
    return device_ids;
  }

  public void release() {
    Stream.of(device_ids).forEach(CL::clReleaseDevice);
    clReleaseCommandQueue(queue);
    clReleaseContext(context);
  }

  public SharedVBO createSharedFloatBuffer(int length, int glBuffer) {
    cl_mem result = clCreateFromGLBuffer(context, CL_MEM_READ_WRITE, glBuffer, error_code_ret);
    check(error_code_ret);
    return SharedVBO.of(length, glBuffer, result);
  }

  public cl_mem createFloat2DImageFromBuffer(
      float[] data,
      int width,
      int height
  ) {
    cl_image_format image_format = imageFormatFloat();
    cl_image_desc image_desc = imageDesc2DFloat(width, height);
    cl_mem result = clCreateImage(
        context,
        CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
        image_format,
        image_desc,
        Pointer.to(data),
        error_code_ret);
    check(error_code_ret);
    return result;
  }

  public cl_mem createFloat2DImageFromEmpty(int width, int height) {
    return create2DImageFromEmpty(width, height, imageFormatFloat());
  }

  public cl_mem createInt2DImageFromEmpty(int width, int height) {
    return create2DImageFromEmpty(width, height, imageFormatInt());
  }

  private cl_mem create2DImageFromEmpty(int width, int height, cl_image_format image_format) {
    cl_image_desc image_desc = imageDesc2DFloat(width, height);
    image_desc.image_row_pitch = 0;
    cl_mem result = clCreateImage(
        context,
        CL_MEM_READ_WRITE,
        image_format,
        image_desc,
        null,
        error_code_ret
    );
    check(error_code_ret);
    return result;
  }

  public FloatBuffer1D createFloatBuffer(int length) {
    cl_mem result = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_float * length, null, error_code_ret);
    check(error_code_ret);
    return FloatBuffer1D.of(result, length);
  }

  public float[] readFloat2DImage(cl_mem image, int width, int height) {
    FloatBuffer buf = FloatBuffer.allocate(width * height);
    read2DImage(image, width, height, Sizeof.cl_float, buf);
    return buf.array();
  }

  public int[] readInt2DImage(cl_mem image, int width, int height) {
    IntBuffer buf = IntBuffer.allocate(width * height);
    read2DImage(image, width, height, Sizeof.cl_int, buf);
    return buf.array();
  }

  public void read2DImage(cl_mem image, int width, int height, int pixelSize, Buffer buffer) {
    clEnqueueReadImage(queue, image, CL_TRUE,
        new long[] {0, 0, 0},
        new long[] {width, height, 1},
        width * pixelSize,
        0,
        Pointer.to(buffer),
        0,
        null,
        null);
    clFinish(queue);
  }

  public float[] readFloatBuffer(SizedBuffer1D buffer) {
    FloatBuffer buf = FloatBuffer.allocate(buffer.length());
    clEnqueueReadBuffer(queue, buffer.buffer(), CL_TRUE, 0, buffer.length() * Sizeof.cl_float, Pointer.to(buf),
        0, null, null);
    clFinish(queue);
    return buf.array();
  }

  public static cl_image_format imageFormatFloat() {
    cl_image_format image_format = new cl_image_format();
    image_format.image_channel_data_type = CL_FLOAT;
    image_format.image_channel_order = CL_R;
    return image_format;
  }

  public static cl_image_format imageFormatInt() {
    cl_image_format image_format = new cl_image_format();
    image_format.image_channel_data_type = CL_SIGNED_INT32;
    image_format.image_channel_order = CL_R;
    return image_format;
  }

  public static cl_image_desc imageDesc2DFloat(int width, int height) {
    cl_image_desc image_desc = new cl_image_desc();
    image_desc.image_type = CL_MEM_OBJECT_IMAGE2D;
    image_desc.image_width = width;
    image_desc.image_height = height;
    image_desc.image_row_pitch = width * Sizeof.cl_float;
    return image_desc;
  }

  public static Session create() {
    int[] error_code_ret = new int[1];
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

    cl_context context = clCreateContext(contextProperties, numDevices, devices, new CreateContextFunction() {
      public void function(String s, Pointer pointer, long l, Object o) {
      }
    }, null, error_code_ret);
    check(error_code_ret);
    cl_queue_properties queue_properties = new cl_queue_properties();
    cl_command_queue queue = clCreateCommandQueue(context, devices[0],
        CL_QUEUE_PROFILING_ENABLE, error_code_ret);

    return new Session(context, queue, devices);
  }

  public static Session createFromGL() {
    int[] error_code_ret = new int[1];
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

    cl_context context = clCreateContext(contextProperties, numDevices, devices, new CreateContextFunction() {
      public void function(String s, Pointer pointer, long l, Object o) {
      }
    }, null, error_code_ret);
    check(error_code_ret);
    cl_command_queue queue = clCreateCommandQueue(context, devices[0],
        CL_QUEUE_PROFILING_ENABLE, error_code_ret);

    return new Session(context, queue, devices);
  }
}
