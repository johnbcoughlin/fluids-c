package jack.fluids.kernels;

import com.google.common.collect.ImmutableList;
import jack.fluids.buffers.SplitBuffer;
import org.assertj.core.data.Offset;
import org.jocl.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static jack.fluids.JOCLUtils.check;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jocl.CL.*;

public class SolidObjectBoundariesKernelTest {
  cl_program program;
  cl_context context;
  cl_command_queue queue;
  int[] error_code_ret = new int[1];

  ComputeSolidObjectBoundariesKernel kernel;

  @Before
  public void before() {
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

    kernel = new ComputeSolidObjectBoundariesKernel(context, queue, devices);
    kernel.compile();
  }

  @After
  public void after() {
    clReleaseCommandQueue(queue);
    clReleaseContext(context);
  }

  @Test
  public void testSinglePositiveDiagonal() {
    int segmentCount = 1;
    int vertexCount = 2;
    float[] vertices = new float[] {
        0.2f, 0.1f, 0.8f, 0.7f
    };
    cl_mem vertexBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
        Sizeof.cl_float * 2 * vertexCount, Pointer.to(vertices), error_code_ret);
    check(error_code_ret);
    cl_mem boundaryPointCountBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_int * segmentCount, null, error_code_ret);
    check(error_code_ret);
    cl_mem boundaryPointCountPrefixSumBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_int * segmentCount, null, error_code_ret);
    check(error_code_ret);

    cl_mem boundaryPointsBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_float * 1000, null, error_code_ret);
    check(error_code_ret);
    SplitBuffer splitBuffer = new SplitBuffer(boundaryPointsBuffer, Sizeof.cl_float * 1000);
    float[] actual = kernel.compute(
        ImmutableList.of(vertexBuffer),
        ImmutableList.of(segmentCount),
        ImmutableList.of(boundaryPointCountBuffer),
        ImmutableList.of(boundaryPointCountPrefixSumBuffer),
        splitBuffer,
        2);

    assertThat(actual).containsExactly(new float[] {1.0f, 0.8f, 1.2f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void testSingleVertical() {
    int segmentCount = 1;
    int vertexCount = 2;
    float[] vertices = new float[] {
        0.2f, 0.1f, 0.2f, 0.8f
    };
    cl_mem vertexBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
        Sizeof.cl_float * 2 * vertexCount, Pointer.to(vertices), error_code_ret);
    check(error_code_ret);
    cl_mem boundaryPointCountBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_int * segmentCount, null, error_code_ret);
    check(error_code_ret);
    cl_mem boundaryPointCountPrefixSumBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_int * segmentCount, null, error_code_ret);
    check(error_code_ret);

    cl_mem boundaryPointsBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_float * 1000, null, error_code_ret);
    check(error_code_ret);
    SplitBuffer splitBuffer = new SplitBuffer(boundaryPointsBuffer, Sizeof.cl_float * 1000);
    float[] actual = kernel.compute(
        ImmutableList.of(vertexBuffer),
        ImmutableList.of(segmentCount),
        ImmutableList.of(boundaryPointCountBuffer),
        ImmutableList.of(boundaryPointCountPrefixSumBuffer),
        splitBuffer,
        4);

    assertThat(actual).containsExactly(new float[] {0.8f, 1.0f, 0.8f, 2.0f, 0.8f, 3.0f}, Offset.offset(0.0001f));
  }
}
