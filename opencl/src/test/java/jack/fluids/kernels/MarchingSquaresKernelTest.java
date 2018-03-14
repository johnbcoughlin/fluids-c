package jack.fluids.kernels;

import jack.fluids.buffers.HistogramPyramid;
import jack.fluids.cl.Session;
import org.jocl.cl_mem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class MarchingSquaresKernelTest {
  private static final int[] error_code_ret = new int[1];

  Session session;

  MarchingSquaresKernel kernel;

  @Before
  public void before() {
    session = Session.create();
    kernel = new MarchingSquaresKernel(session);
    kernel.compile();
  }

  @After
  public void after() {
    session.release();
  }

  @Test
  public void test() {
    int width = 10;
    int height = 10;
    float[] phiData = new float[width * height];
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        phiData[j * width + i] = (float) Math.sqrt(
            Math.pow(5.0f - i, 2) + Math.pow(5.0f - j, 2)) - 3.0f;
      }
    }

    cl_mem phi = session.createFloat2DImageFromBuffer(phiData, width, height);
    HistogramPyramid hp = HistogramPyramid.create(session, width, height);
    System.out.println(hp);

    kernel.march(phi, hp, width, height);

    int[] actual = session.readInt2DImage(hp.bottom(), hp.width(0), hp.height(0));
    System.out.println(Arrays.toString(actual));
  }
}
