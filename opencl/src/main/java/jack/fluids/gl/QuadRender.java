package jack.fluids.gl;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

public class QuadRender {
  private final GLAutoDrawable drawable;
  private final GL4 gl;
  private final int texture;

  private int vao;
  private int buffer;
  private int program;
  private int uniformLocation;

  public QuadRender(GLAutoDrawable drawable, int texture) {
    this.drawable = drawable;
    this.gl = drawable.getGL().getGL4();
    this.texture = texture;
  }

  public void setup() {
    int vertexShader = GLUtils.loadShader(gl, GL4.GL_VERTEX_SHADER, vertexShaderSource());
    int fragmentShader = GLUtils.loadShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentShaderSource());
    program = GLUtils.compileProgram(gl, vertexShader, fragmentShader);

    gl.glUseProgram(program);
    buffer = GLUtils.createBuffer(gl);

    vao = GLUtils.createVAO(gl);
    gl.glBindVertexArray(vao);
    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, buffer);
    gl.glBufferData(GL4.GL_ARRAY_BUFFER, Float.BYTES * 12, quadVertices(), GL4.GL_STATIC_DRAW);
    int coordsAttributeLocation = gl.glGetAttribLocation(program, "a_coords");
    gl.glEnableVertexAttribArray(coordsAttributeLocation);
    gl.glVertexAttribPointer(coordsAttributeLocation, 2, gl.GL_FLOAT, false, 0, 0);
    gl.glBindVertexArray(0);

    uniformLocation = gl.glGetUniformLocation(program, "u_texture");
    GLUtils.check(gl);
  }

  public void draw() {
    gl.glUseProgram(program);

    gl.glBindVertexArray(vao);

    gl.glUniform1i(uniformLocation, 0);
    gl.glClearColor(1.0f, 0.6f, 0.4f, 0.5f);
    gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glBindTexture(GL4.GL_TEXTURE_2D, texture);

    gl.glDrawArrays(GL4.GL_TRIANGLES, 0, 6);
    gl.glBindVertexArray(0);

    drawable.swapBuffers();
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
