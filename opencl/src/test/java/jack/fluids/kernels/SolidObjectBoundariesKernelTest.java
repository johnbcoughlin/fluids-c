package jack.fluids.kernels;

import com.google.common.collect.ImmutableList;
import jack.fluids.buffers.FloatBuffer1D;
import jack.fluids.buffers.IntBuffer1D;
import jack.fluids.buffers.SplitBuffer;
import jack.fluids.cl.Session;
import org.assertj.core.data.Offset;
import org.jocl.Sizeof;
import org.jocl.cl_mem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static jack.fluids.cl.JOCLUtils.check;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jocl.CL.*;

public class SolidObjectBoundariesKernelTest {
  int[] error_code_ret = new int[1];

  Session session;
  ComputeSolidObjectBoundariesKernel kernel;

  FloatBuffer1D vertexBuffer;
  IntBuffer1D boundaryPointCountBuffer;
  IntBuffer1D boundaryPointCountPrefixSumBuffer;
  cl_mem boundaryPointsBuffer;
  SplitBuffer boundaryPointsSplitBuffer;

  @Before
  public void before() {
    session = Session.create();
    kernel = new ComputeSolidObjectBoundariesKernel(session);
    kernel.compile();
  }

  @After
  public void after() {
    clReleaseMemObject(vertexBuffer.buffer());
    clReleaseMemObject(boundaryPointCountBuffer.buffer());
    clReleaseMemObject(boundaryPointCountPrefixSumBuffer.buffer());
    clReleaseMemObject(boundaryPointsBuffer);
    session.release();
  }

  @Test
  public void testSinglePositiveDiagonal() {
    float[] vertices = new float[]{
        0.4f, 0.2f, 1.6f, 1.4f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 0.8f, 1.2f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void testSingleNegativeDiagonal() {
    float[] vertices = new float[]{
        1.2f, 1.4f, 0.2f, 0.2f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 1.16f, 0.8666f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void testSingleVertical() {
    float[] vertices = new float[]{
        0.8f, 0.4f, 0.8f, 3.2f
    };
    float[] actual = boundaryPointsFor(1, vertices, 4);
    assertThat(actual).containsExactly(new float[]{
        0.8f, 1.0f, 0.8f, 2.0f, 0.8f, 3.0f
    }, Offset.offset(0.0001f));
  }

  @Test
  public void testNearlyVertical() {
    float[] vertices = new float[]{
        0.9999f, 0.2f, 1.0001f, 0.8f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void testNearlyHorizontal() {
    float[] vertices = new float[]{
        0.2f, 0.9999f, 0.8f, 1.0001f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void testLoop() {
    float[] vertices = new float[]{
        0.2f, 0.2f, 1.6f, 0.4f, 1.2f, 1.4f, 0.2f, 0.2f
    };
    float[] actual = boundaryPointsFor(3, vertices, 2);
    assertThat(actual).containsExactly(new float[]{
        1.0f, 0.31428573f, 1.36f, 1.0f, 1.0f, 1.16f, 0.8666667f, 1.0f
    }, Offset.offset(0.0001f));
  }

  @Test
  public void testTooManyPointsInVerticalSegment() {
    float[] vertices = new float[]{
        0.3f * 128, 0.1f * 128, 0.35f * 128, 0.9f * 128
    };
    assertThatThrownBy(() -> boundaryPointsFor(1, vertices, 128))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("too many intersections in one segment");
    // doesn't throw
    float[] vertices2 = new float[]{
        0.3f * 64, 0.1f * 64, 0.35f * 64, 0.9f * 64
    };
    boundaryPointsFor(1, vertices2, 64);
  }

  @Test
  public void testTooManyPointsInHorizontalSegment() {
    float[] vertices = new float[]{
        0.1f * 128, 0.3f * 128, 0.9f * 128, 0.35f * 128
    };
    assertThatThrownBy(() -> boundaryPointsFor(1, vertices, 128))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("too many intersections in one segment");
    // doesn't throw
    float[] vertices2 = new float[]{
        0.1f * 64, 0.3f * 64, 0.9f * 64, 0.35f * 64
    };
    boundaryPointsFor(1, vertices2, 64);
  }

  @Test
  public void cullsEquivalentHorizontalAndVerticalIntersections() {
    float[] vertices = new float[]{
        0.2f, 0.2f, 1.6f, 1.6f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void cullsVeryClosePoints() {
    float[] vertices = new float[]{
        0.2f, 0.2f, 1.6f, 1.6001f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 1.00011f}, Offset.offset(0.0001f));
  }

  @Test
  public void doesNotCullPrettyClosePoints() {
    float[] vertices = new float[]{
        0.2f, 0.2f, 1.6f, 1.61f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{0.9943f, 1.0f, 1.0f, 1.0057f}, Offset.offset(0.0001f));
  }

  @Test
  public void verticalIntersectionAtEndVertex() {
    float[] vertices = new float[]{
        0.2f, 0.2f, 1.0f, 0.8f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void verticalIntersectionAtStartVertex() {
    float[] vertices = new float[]{
        1.0f, 0.2f, 1.6f, 0.8f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{1.0f, 0.2f}, Offset.offset(0.0001f));
  }

  @Test
  public void horizontalIntersectionAtStartVertex() {
    float[] vertices = new float[]{
        0.2f, 1.0f, 0.4f, 1.8f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).containsExactly(new float[]{0.2f, 1.0f}, Offset.offset(0.0001f));
  }

  @Test
  public void horizontalIntersectionAtEndVertex() {
    float[] vertices = new float[]{
        0.2f, 0.2f, 0.4f, 1.0f
    };
    float[] actual = boundaryPointsFor(1, vertices, 2);
    assertThat(actual).isEmpty();
  }

  @Test
  public void verticalIntersectionAtInteriorVertex() {
    float[] vertices = new float[]{
        0.2f, 0.2f, 1.0f, 0.6f, 1.6f, 0.8f
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
      vertices[2 * i] = (float) (0.5 + 0.5 * Math.cos(theta)) * 64;
      vertices[2 * i + 1] = (float) (0.5 + 0.5 * Math.sin(theta)) * 64;
    }
    vertices[2 * segmentCount] = vertices[0];
    vertices[2 * segmentCount + 1] = vertices[1];
    System.out.println(Arrays.toString(vertices));
    float[] actual = boundaryPointsFor(segmentCount, vertices, 64);
  }

  private float[] boundaryPointsFor(int segmentCount, float[] vertices, int invMeshSize) {
    vertexBuffer = session.createFloatBuffer(vertices);
    boundaryPointCountBuffer = session.createIntBuffer(segmentCount);
    boundaryPointCountPrefixSumBuffer = session.createIntBuffer(segmentCount);
    createSplitBuffer(1000);
    return session.readFloatBuffer(kernel.compute(
        ImmutableList.of(vertexBuffer.buffer()),
        ImmutableList.of(segmentCount),
        ImmutableList.of(boundaryPointCountBuffer.buffer()),
        ImmutableList.of(boundaryPointCountPrefixSumBuffer.buffer()),
        boundaryPointsSplitBuffer,
        invMeshSize,
        invMeshSize));
  }

  void createSplitBuffer(long size) {
    boundaryPointsBuffer = clCreateBuffer(session.context(), CL_MEM_READ_WRITE,
        Sizeof.cl_float * size, null, error_code_ret);
    check(error_code_ret);
    boundaryPointsSplitBuffer = new SplitBuffer(boundaryPointsBuffer, Sizeof.cl_float * size);
  }
}
