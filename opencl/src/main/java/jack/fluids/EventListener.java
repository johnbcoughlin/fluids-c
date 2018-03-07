package jack.fluids;

import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import jogamp.opengl.macosx.cgl.CGL;

import java.nio.FloatBuffer;
import java.util.function.LongConsumer;

import static com.jogamp.opengl.GL.*;


public class EventListener implements GLEventListener, KeyListener {
  private final LongConsumer shareGroupConsumer;

  public EventListener(LongConsumer shareGroupConsumer) {
    this.shareGroupConsumer = shareGroupConsumer;
  }

  public void init(GLAutoDrawable drawable) {
    GL4 gl = drawable.getGL().getGL4();

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
    System.out.println("texture: " + textures[0]);

    gl.glFinish();

    long cglCtx = CGL.CGLGetCurrentContext();
    long shareGroup = CGL.CGLGetShareGroup(cglCtx);
    shareGroupConsumer.accept(shareGroup);
  }

  public void dispose(GLAutoDrawable drawable) {
  }

  public void display(GLAutoDrawable drawable) {
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
  }

  public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
  }

  public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
  }
}
