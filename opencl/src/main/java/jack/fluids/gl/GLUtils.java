package jack.fluids.gl;

import com.google.common.base.Charsets;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;

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

  public static int createTextureWithData(GL4 gl, int width, int height, FloatBuffer data) {
    int texture = createTexture(gl);
    gl.glBindTexture(GL_TEXTURE_2D, texture);
    gl.glTexImage2D(GL_TEXTURE_2D, 0, GL4.GL_RGBA32F, width, height, 0, gl.GL_RGBA, gl.GL_FLOAT, data);
    gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    return texture;
  }

  public static int createVAO(GL4 gl) {
    int[] ret = new int[1];
    gl.glGenVertexArrays(1, ret, 0);
    return ret[0];
  }

  public static int loadShader(GL4 gl, int type, String source) {
    int shader = gl.glCreateShader(type);
    gl.glShaderSource(shader, 1, new String[]{source}, new int[]{source.length()}, 0);
    gl.glCompileShader(shader);
    check(gl);
    IntBuffer sizeBuffer = IntBuffer.allocate(1);
    gl.glGetShaderiv(shader, GL4.GL_INFO_LOG_LENGTH, sizeBuffer);
    int size = sizeBuffer.get(0);
    if (size > 0) {
      ByteBuffer buffer = ByteBuffer.allocate(size);
      gl.glGetShaderInfoLog(shader, size, sizeBuffer, buffer);
      String error =new String(buffer.array(), Charsets.UTF_8);
      System.err.println("Compile error in shader: ");
      System.err.println(error);
      throw new RuntimeException();
    }
    return shader;
  }

  public static int compileProgram(GL4 gl, int vertexShader, int fragmentShader) {
    int program = gl.glCreateProgram();
    gl.glAttachShader(program, vertexShader);
    gl.glAttachShader(program, fragmentShader);
    gl.glLinkProgram(program);
    check(gl);
    IntBuffer sizeBuffer = IntBuffer.allocate(1);
    gl.glGetProgramiv(program, GL4.GL_INFO_LOG_LENGTH, sizeBuffer);
    int size = sizeBuffer.get(0);
    if (size > 0) {
      ByteBuffer buffer = ByteBuffer.allocate(size);
      gl.glGetProgramInfoLog(program, size, sizeBuffer, buffer);
      System.err.println("Error linking program: ");
      String error = new String(buffer.array(), Charsets.UTF_8);
      System.err.println(error);
      throw new RuntimeException();
    }
    return program;
  }

  public static void check(GL4 gl) {
    int errorCode = gl.glGetError();
    if (errorCode != GL.GL_NO_ERROR) {
      throw new RuntimeException("gl error code: " + errorCode);
    }
  }
}
