package jack.fluids.glutils;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

public class GLUtils {
  private GLUtils() {
  }

  public static int createBuffer(GL4 gl) {
    int[] ret = new int[1];
    gl.glGenBuffers(1, ret, 0);
    return ret[0];
  }

  public static int createTexture(GL4 gl) {
    int[] ret = new int[1];
    gl.glGenTextures(1, ret, 0);
    return ret[0];
  }

  public static int loadShader(GL4 gl, int type, String source) {
    int shader = gl.glCreateShader(type);
    gl.glShaderSource(shader, 1, new String[] {source}, new int[] {source.length()}, 0);
    gl.glCompileShader(shader);
    check(gl);
    return shader;
  }

  public static int compileProgram(GL4 gl, int vertexShader, int fragmentShader) {
    int program = gl.glCreateProgram();
    gl.glAttachShader(program, vertexShader);
    gl.glAttachShader(program, fragmentShader);
    gl.glLinkProgram(program);
    check(gl);
    return program;
  }

  public static void check(GL4 gl) {
    int errorCode = gl.glGetError();
    if (errorCode != GL.GL_NO_ERROR) {
      throw new RuntimeException("gl error code: " + errorCode);
    }
  }
}
