package jack.fluids.kernels;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class GLWindowRule implements TestRule {
  private final AtomicReference<GLAutoDrawable> drawableReference;

  public GLWindowRule(AtomicReference<GLAutoDrawable> drawableReference) {
    this.drawableReference = drawableReference;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    System.out.println("applying rule");
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        System.out.println("evaluating ruled version");
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities capabilities = new GLCapabilities(profile);
        GLWindow window = GLWindow.create(capabilities);

        window.setTitle(description.getDisplayName());
        window.setSize(500, 500);
        window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        window.setVisible(true);

        final Animator animator = new Animator(window);

        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        window.addGLEventListener(new GLEventListener() {
          @Override
          public void init(GLAutoDrawable drawable) {
            drawable.setAutoSwapBufferMode(false);
            drawableReference.set(drawable);
            try {
              statement.evaluate();
            } catch (Throwable throwable) {
              animator.stop();
              completionFuture.completeExceptionally(throwable);
            }
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
        });

        window.addKeyListener(new KeyListener() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyChar() == 'w') {
              animator.stop();
              completionFuture.complete(null);
            }
          }

          @Override
          public void keyReleased(KeyEvent e) {

          }
        });

        animator.start();

        window.addWindowListener(new WindowAdapter() {
          @Override
          public void windowDestroyed(WindowEvent e) {
            animator.stop();
            completionFuture.complete(null);
          }
        });
        completionFuture.join();
      }
    };
  }
}
