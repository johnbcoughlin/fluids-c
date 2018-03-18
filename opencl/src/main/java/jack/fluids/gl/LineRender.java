package jack.fluids.gl;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import java.io.IOException;
import java.io.InputStreamReader;

public class LineRender {
  private final GLAutoDrawable drawable;
  private final GL4 gl;
  private final int vbo;
  private final int vertexCount;
  private final float width;
  private final float height;

  private int program;
  private int vao;
  private int toClipcoordsLocation;

  public LineRender(GLAutoDrawable drawable, GL4 gl, int vbo, int vertexCount, float width, float height) {
    this.drawable = drawable;
    this.gl = gl;
    this.vbo = vbo;
    this.vertexCount = vertexCount;
    this.width = width;
    this.height = height;
  }

  public void setup() {
    int vertexShader = GLUtils.loadShader(gl, GL4.GL_VERTEX_SHADER, vertexShaderSource());
    int fragmentShader = GLUtils.loadShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentShaderSource());
    program = GLUtils.compileProgram(gl, vertexShader, fragmentShader);

    gl.glUseProgram(program);

    vao = GLUtils.createVAO(gl);
    gl.glBindVertexArray(vao);
    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
    int coordsAttributeLocation = gl.glGetAttribLocation(program, "a_coords");
    gl.glEnableVertexAttribArray(coordsAttributeLocation);
    gl.glVertexAttribPointer(coordsAttributeLocation, 2, gl.GL_FLOAT, false, 0, 0);
    gl.glBindVertexArray(0);

    toClipcoordsLocation = gl.glGetUniformLocation(program, "toClipcoords");
    gl.glUniformMatrix4fv(toClipcoordsLocation, 1, false, Matrices.toClipcoords(width, height));
  }

  public void draw() {
    draw(0);
  }

  public void draw(int offset) {
    gl.glUseProgram(program);
    gl.glBindVertexArray(vao);
    gl.glClearColor(0, 0, 0, 0);
    gl.glDrawArrays(GL4.GL_LINES, offset, vertexCount);
    gl.glBindVertexArray(0);
    drawable.swapBuffers();
  }

  private static String vertexShaderSource() {
    try {
      return CharStreams.toString(new InputStreamReader(
          QuadRender.class.getResourceAsStream("/shaders/line/vertex.vtx")));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static String fragmentShaderSource() {
    try {
      return CharStreams.toString(new InputStreamReader(
          QuadRender.class.getResourceAsStream("/shaders/line/white.frag")));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
