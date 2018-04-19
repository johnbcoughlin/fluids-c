package jack.fluids;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import jack.fluids.cl.Session;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;

public class FluidsApp implements GLEventListener, KeyListener {
  private CLSimulation simulation;

  @Override
  public void keyPressed(KeyEvent e) {

  }

  @Override
  public void keyReleased(KeyEvent e) {

  }

  @Override
  public void init(GLAutoDrawable drawable) {
    GL4 gl = drawable.getGL().getGL4();
    simulation = new CLSimulation(gl, Session.createFromGL(), 100, 100);

    int[] textures = new int[1];
    gl.glGenTextures(1, textures, 0);
    gl.glBindTexture(GL_TEXTURE_2D, textures[0]);
    float[] array = new float[40000];
    for (int i = 0; i < 100; i++) {
      for (int j = 0; j < 100; j++) {
        array[i * 100 + j] = (float) Math.random();
        array[i * 100 + j + 1] = (float) Math.random();
        array[i * 100 + j + 2] = (float) Math.random();
        array[i * 100 + j + 3] = (float) Math.random();
      }
    }
    FloatBuffer buffer = FloatBuffer.wrap(array);
    gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, 100, 100, 0, GL_RGBA, GL_FLOAT, buffer);
    gl.glFinish();

    simulation.init();
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {

  }

  @Override
  public void display(GLAutoDrawable drawable) {

  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

  }
}
