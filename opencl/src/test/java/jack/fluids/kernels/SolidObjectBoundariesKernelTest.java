package jack.fluids.kernels;

import com.google.common.collect.ImmutableList;
import jack.fluids.buffers.SplitBuffer;
import org.assertj.core.data.Offset;
import org.jocl.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static jack.fluids.cl.JOCLUtils.check;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jocl.CL.*;

public class SolidObjectBoundariesKernelTest {
  cl_context context;
  cl_command_queue queue;
  int[] error_code_ret = new int[1];

  ComputeSolidObjectBoundariesKernel kernel;

  cl_mem vertexBuffer;
  cl_mem boundaryPointCountBuffer;
  cl_mem boundaryPointCountPrefixSumBuffer;
  cl_mem boundaryPointsBuffer;
  SplitBuffer boundaryPointsSplitBuffer;

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
    clReleaseMemObject(vertexBuffer);
    clReleaseMemObject(boundaryPointCountBuffer);
    clReleaseMemObject(boundaryPointCountPrefixSumBuffer);
    clReleaseMemObject(boundaryPointsBuffer);
    clReleaseCommandQueue(queue);
    clReleaseContext(context);
  }

  @Test
  public void testSinglePositiveDiagonal() {
    float[] vertices = new float[]{
        0.2f, 0.1f, 0.8f, 0.7f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 0.8f, 1.2f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void testSingleNegativeDiagonal() {
    float[] vertices = new float[]{
        0.6f, 0.7f, 0.1f, 0.1f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 1.16f, 0.8666f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void testSingleVertical() {
    float[] vertices = new float[]{
        0.2f, 0.1f, 0.2f, 0.8f
    };
    float[] actual = boundaryPointsFor(1, vertices, 4);
    assertThat(actual).containsExactly(new float[]{
        0.8f, 1.0f, 0.8f, 2.0f, 0.8f, 3.0f
    }, Offset.offset(0.0001f));
  }

  @Test
  public void testNearlyVertical() {
    float[] vertices = new float[]{
        0.4999f, 0.1f, 0.5001f, 0.4f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void testNearlyHorizontal() {
    float[] vertices = new float[]{
        0.1f, 0.4999f, 0.4f, 0.5001f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void testLoop() {
    float[] vertices = new float[]{
        0.1f, 0.1f, 0.8f, 0.2f, 0.6f, 0.7f, 0.1f, 0.1f
    };
    float[] actual = boundaryPointsFor(3, vertices, 2);
    assertThat(actual).containsExactly(new float[]{
        1.0f, 0.31428573f, 1.36f, 1.0f, 1.0f, 1.16f, 0.8666667f, 1.0f
    }, Offset.offset(0.0001f));
  }

  @Test
  public void testTooManyPointsInVerticalSegment() {
    float[] vertices = new float[]{
        0.3f, 0.1f, 0.35f, 0.9f
    };
    assertThatThrownBy(() -> boundaryPointsFor(1, vertices, 128))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("too many intersections in one segment");
    // doesn't throw
    boundaryPointsFor(1, vertices, 64);
  }

  @Test
  public void testTooManyPointsInHorizontalSegment() {
    float[] vertices = new float[]{
        0.1f, 0.3f, 0.9f, 0.35f
    };
    assertThatThrownBy(() -> boundaryPointsFor(1, vertices, 128))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("too many intersections in one segment");
    // doesn't throw
    boundaryPointsFor(1, vertices, 64);
  }

  @Test
  public void cullsEquivalentHorizontalAndVerticalIntersections() {
    float[] vertices = new float[]{
        0.1f, 0.1f, 0.8f, 0.8f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void cullsVeryClosePoints() {
    float[] vertices = new float[]{
        0.1f, 0.1f, 0.8f, 0.8001f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 1.00011f}, Offset.offset(0.0001f));
  }

  @Test
  public void doesNotCullPrettyClosePoints() {
    float[] vertices = new float[]{
        0.1f, 0.1f, 0.8f, 0.81f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{0.9887f, 1.0f, 1.0f, 1.0114f}, Offset.offset(0.0001f));
  }

  @Test
  public void verticalIntersectionAtEndVertex() {
    float[] vertices = new float[]{
        0.1f, 0.1f, 0.5f, 0.4f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void verticalIntersectionAtStartVertex() {
    float[] vertices = new float[]{
        0.5f, 0.1f, 0.8f, 0.4f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 0.2f}, Offset.offset(0.0001f));
  }

  @Test
  public void horizontalIntersectionAtStartVertex() {
    float[] vertices = new float[]{
        0.1f, 0.5f, 0.2f, 0.9f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{0.2f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void horizontalIntersectionAtEndVertex() {
    float[] vertices = new float[]{
        0.1f, 0.1f, 0.2f, 0.5f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void verticalIntersectionAtInteriorVertex() {
    float[] vertices = new float[]{
        0.1f, 0.1f, 0.5f, 0.3f, 0.8f, 0.4f
    };
    float[] actual = boundaryPointsFor(2, vertices, 2);
    assertThat(actual).containsExactly(new float[] {1.0f, 0.6f}, Offset.offset(0.0001f));
  }

  @Test
  public void stressTest() {
    int segmentCount = 300;
    float[] vertices = new float[2 * segmentCount + 2];
    for (int i = 0; i < segmentCount; i++) {
      double theta = (double) i / segmentCount * 2 * Math.PI;
      vertices[2 * i] = (float) (0.5 + 0.5 * Math.cos(theta));
      vertices[2 * i + 1] = (float) (0.5 + 0.5 * Math.sin(theta));
    }
    vertices[2 * segmentCount] = vertices[0];
    vertices[2 * segmentCount + 1] = vertices[1];
    System.out.println(Arrays.toString(vertices));
    float[] actual = boundaryPointsFor(segmentCount, vertices, 64);
  }

  private float[] boundaryPointsFor(int segmentCount, float[] vertices, int invMeshSize) {
    vertexBuffer = createVertexBuffer(vertices);
    boundaryPointCountBuffer = createIntBuffer(segmentCount);
    boundaryPointCountPrefixSumBuffer = createIntBuffer(segmentCount);
    createSplitBuffer(1000);
    return kernel.compute(
        ImmutableList.of(vertexBuffer),
        ImmutableList.of(segmentCount),
        ImmutableList.of(boundaryPointCountBuffer),
        ImmutableList.of(boundaryPointCountPrefixSumBuffer),
        boundaryPointsSplitBuffer,
        invMeshSize);
  }

  void createSplitBuffer(long size) {
    boundaryPointsBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_float * size, null, error_code_ret);
    check(error_code_ret);
    boundaryPointsSplitBuffer = new SplitBuffer(boundaryPointsBuffer, Sizeof.cl_float * 1000);
  }

  cl_mem createVertexBuffer(float[] floats) {
    cl_mem result = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
        Sizeof.cl_float * floats.length, Pointer.to(floats), error_code_ret);
    check(error_code_ret);
    return result;
  }

  cl_mem createIntBuffer(int size) {
    cl_mem result = clCreateBuffer(context, CL_MEM_READ_WRITE,
        Sizeof.cl_int * size, null, error_code_ret);
    check(error_code_ret);
    return result;
  }
}
