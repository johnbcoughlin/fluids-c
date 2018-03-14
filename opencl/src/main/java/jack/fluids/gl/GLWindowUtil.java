package jack.fluids.gl;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;

import java.util.concurrent.CompletableFuture;

public class GLWindowUtil implements GLEventListener {
  private GL4 gl;
  private CompletableFuture<GL4> setupFuture;

  public GLWindowUtil() {

  }

  public CompletableFuture<GL4> setup() {
    this.setupFuture = new CompletableFuture<>();
    GLProfile profile = GLProfile.get(GLProfile.GL4);
    GLCapabilities capabilities = new GLCapabilities(profile);
    GLWindow window = GLWindow.create(capabilities);

    window.setTitle("a triangle");
    window.setSize(200, 200);
    window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
    window.setVisible(true);

    window.addGLEventListener(this);


    final Animator animator = new Animator(window);
    animator.start();

    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowDestroyed(WindowEvent e) {
        animator.stop();
        System.exit(0);
      }
    });

    return this.setupFuture;
  }

  @Override
  public void init(GLAutoDrawable drawable) {
    this.gl = drawable.getGL().getGL4();
    this.setupFuture.complete(gl);
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {
    System.out.println("here");
  }

  @Override
  public void display(GLAutoDrawable drawable) {

  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

  }
}
