package jack.fluids.kernels;

import com.google.common.collect.ImmutableList;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import jack.fluids.buffers.TwoPhaseBuffer;
import jack.fluids.cl.Session;
import jack.fluids.gl.GLUtils;
import jack.fluids.gl.QuadRender;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_mem;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static jack.fluids.cl.JOCLUtils.check;
import static org.jocl.CL.*;

public class WindingNumberKernelTest {
  static final int[] error_code_ret = new int[1];

  AtomicReference<GLAutoDrawable> drawable = new AtomicReference<>();

  @Rule
  public final TestRule rule = new GLWindowRule(drawable);

  GL4 gl;
  QuadRender quadRender;
  Session session;
  ComputeWindingNumberKernel kernel;

  @Before
  public void before() {
    session = Session.createFromGL();
    kernel = new ComputeWindingNumberKernel(session);
    kernel.compile();
    this.gl = drawable.get().getGL().getGL4();
  }

  @After
  public void after() {
    session.release();
  }

  @Test
  public void test() {
    int width = 100;
    int height = 100;

    int texture1 = GLUtils.createTextureWithData1i(gl, width, height, IntBuffer.allocate(width * height));
    GLUtils.check(gl);
    int texture2 = GLUtils.createTextureWithData1i(gl, width, height, IntBuffer.allocate(width * height));
    GLUtils.check(gl);
    cl_mem image1 = clCreateFromGLTexture(session.context(), CL_MEM_READ_WRITE, GL4.GL_TEXTURE_2D, 0,
        texture1, error_code_ret);
    check(error_code_ret);
    cl_mem image2 = clCreateFromGLTexture(session.context(), CL_MEM_READ_WRITE, GL4.GL_TEXTURE_2D, 0,
        texture2, error_code_ret);
    check(error_code_ret);

    TwoPhaseBuffer grid = new TwoPhaseBuffer(image1, image2);

    float[] data = new float[] {
        .12f * width, .12f * height,
        .78f * width, .12f * height,
        .78f * width, .12f * height,
        .56f * width, .84f * height,
        .56f * width, .84f * height,
        .12f * width, .12f * height
    };
    cl_mem vbo = clCreateBuffer(session.context(), CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
        data.length * Sizeof.cl_float,
        Pointer.to(data), error_code_ret);
    check(error_code_ret);

    kernel.computeWindingNumbers(
        ImmutableList.of(vbo),
        ImmutableList.of(3),
        ImmutableList.of(4),
        grid,
        new float[] {(float) -Math.random(), (float) -Math.random()},
        width,
        height
    );

    grid.swap();

    System.out.println(Arrays.toString(session.readInt2DImage(grid.front(), width, height)));

    clEnqueueReleaseGLObjects(session.queue(), 2, new cl_mem[]{grid.front(), grid.back()}, 0, null, null);
    clFinish(session.queue());

    quadRender = new QuadRender(drawable.get(), texture2);
    quadRender.setup();
    quadRender.draw();
  }
}
