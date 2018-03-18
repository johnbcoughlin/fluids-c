package jack.fluids.clgl;

import com.jogamp.opengl.GL4;
import jack.fluids.buffers.SharedVBO;
import jack.fluids.cl.Session;
import jack.fluids.gl.GLUtils;

import java.nio.FloatBuffer;

public class CLGLUtils {
  public static SharedVBO createSharedVBO(int length, Session clSession, GL4 gl) {
    FloatBuffer buffer = FloatBuffer.allocate(length);
    int glBuffer = GLUtils.createBufferWithData(gl, length, buffer);
    return clSession.createSharedFloatBuffer(length, glBuffer);
  }

  private CLGLUtils() {}
}
