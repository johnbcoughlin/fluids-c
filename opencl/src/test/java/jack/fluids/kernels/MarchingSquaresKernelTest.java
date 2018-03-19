package jack.fluids.kernels;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import jack.fluids.buffers.HistogramPyramid;
import jack.fluids.buffers.SharedVBO;
import jack.fluids.cl.Session;
import jack.fluids.gl.LineRender;
import jogamp.opengl.macosx.cgl.CGL;
import org.jocl.cl_mem;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class MarchingSquaresKernelTest {
  private static final int[] error_code_ret = new int[1];

  private final AtomicReference<GLAutoDrawable> drawable = new AtomicReference<>();
  GL4 gl;

  @Rule
  public final TestRule rule = new GLWindowRule(drawable);

  Session session;

  MarchingSquaresKernel kernel;

  @Before
  public void before() {
    gl = drawable.get().getGL().getGL4();
    long shareGroup = CGL.CGLGetShareGroup(CGL.CGLGetCurrentContext());
    session = Session.createFromGL();
    kernel = new MarchingSquaresKernel(session, gl);
    kernel.compile();
  }

  @After
  public void after() {
    session.release();
  }

  @Test
  public void testCircle() {
    int width = 100;
    int height = 100;
    float[] phiData = new float[width * height];
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        phiData[j * width + i] = (float) Math.sqrt(
            Math.pow(width / 2.0 - i, 2) + Math.pow(height / 2.0 - j, 2)) - width / 4.0f;
      }
    }

    cl_mem phi = session.createFloat2DImageFromBuffer(phiData, width, height);
    HistogramPyramid hp = HistogramPyramid.create(session, width, height);
    System.out.println(hp);

    SharedVBO vbo = kernel.march(phi, hp, width, height);

    LineRender render = new LineRender(
        drawable.get(),
        gl,
        vbo.glBufferName(),
        vbo.length() / 2,
        width,
        height
    );
    render.setup();
    render.draw();

    float[] floats = session.readFloatBuffer(vbo);

    System.out.println(Arrays.toString(floats));
    System.out.println(floats.length);
    for (int i = 0; i < floats.length; i += 4) {
      System.out.print(", ");
      System.out.print(floats[i]);
    }
    System.out.println();
  }

  @Test
  public void drawHourglass() {
    int width = 60;
    int height = 60;
    float[] phiData = new float[width * height];
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        phiData[j * width + i] = (float) ((i - (width / 3)) * (j - (height / 3)));
      }
    }
    cl_mem phi = session.createFloat2DImageFromBuffer(phiData, width, height);
    HistogramPyramid hp = HistogramPyramid.create(session, width, height);
    System.out.println(hp);

    SharedVBO vbo = kernel.march(phi, hp, width, height);

    LineRender render = new LineRender(
        drawable.get(),
        gl,
        vbo.glBufferName(),
        vbo.length() / 2,
        width,
        height
    );
    render.setup();
    render.draw();

    float[] floats = session.readFloatBuffer(vbo);

    System.out.println(Arrays.toString(floats));
    System.out.println(floats.length);
    for (int i = 0; i < floats.length; i += 4) {
      System.out.print(", ");
      System.out.print(floats[i]);
    }
    System.out.println();
  }

  private void printHistogramPyramid(HistogramPyramid hp) {
    for (int i = 0; i < hp.levelCount(); i++) {
      int[] actual = session.readInt2DImage(hp.level(i), hp.width(i), hp.height(i));
      int rowLength = hp.width(i);
      System.out.print("[\t");
      for (int j = 0; j < actual.length / rowLength; j++) {
        for (int k = 0; k < rowLength; k++) {
          System.out.print(actual[j * rowLength + k] + "\t");
        }
        System.out.print("\n\t");
      }
      System.out.println("");
    }
  }
}
