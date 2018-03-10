package jack.fluids.glutils;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

public class QuadRender {
  private final GL4 gl;
  private final int texture;

  private int buffer;
  private int program;
  private int uniformLocation;

  public QuadRender(GL4 gl, int texture) {
    this.gl = gl;
    this.texture = texture;
  }

  public void setup() {
    int vertexShader = GLUtils.loadShader(gl, GL4.GL_VERTEX_SHADER, vertexShaderSource());
    int fragmentShader = GLUtils.loadShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentShaderSource());
    program = GLUtils.compileProgram(gl, vertexShader, fragmentShader);

    gl.glUseProgram(program);
    buffer = GLUtils.createBuffer(gl);
    System.out.println(buffer);
    System.out.println(GLUtils.createBuffer(gl));
    System.out.println(GLUtils.createBuffer(gl));
    System.out.println(GLUtils.createBuffer(gl));
    System.out.println(GLUtils.createBuffer(gl));
    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 1);
    gl.glBufferData(GL4.GL_ARRAY_BUFFER, Float.BYTES * 12, quadVertices(), GL4.GL_STATIC_DRAW);

    uniformLocation = gl.glGetUniformLocation(program, "u_texture");
  }

  public void draw() {
    gl.glUseProgram(program);

    gl.glUniform1ui(uniformLocation, 0);
    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, buffer);
    gl.glBindTexture(GL4.GL_TEXTURE_2D, texture);

    gl.glClearColor(1.0f, 0.6f, 0.4f, 0.5f);
    gl.glClear(GL3.GL_COLOR_BUFFER_BIT);

//    gl.glDrawArrays(GL4.GL_TRIANGLES, 0, 6);
  }

  public static FloatBuffer quadVertices() {
    return FloatBuffer.wrap(new float[] {
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,

        -1.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, -1.0f
    });
  }

  private static String vertexShaderSource() {
    try {
      return CharStreams.toString(new InputStreamReader(
          QuadRender.class.getResourceAsStream("/shaders/identity.vtx")));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static String fragmentShaderSource() {
    try {
      return CharStreams.toString(new InputStreamReader(
          QuadRender.class.getResourceAsStream("/shaders/sample.frag")));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
