package jack.fluids.kernels;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.CompletableFuture;

public class GLWindowRule implements TestRule {
  private final GLEventListener eventListener;
  private final CompletableFuture<Void> initializationFuture;
  private final CompletableFuture<Void> testCompletionFuture;

  public GLWindowRule(GLEventListener eventListener,
                      CompletableFuture<Void> initializationFuture,
                      CompletableFuture<Void> testCompletionFuture) {
    this.eventListener = eventListener;
    this.initializationFuture = initializationFuture;
    this.testCompletionFuture = testCompletionFuture;
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
        window.setSize(400, 400);
        window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        window.setVisible(true);

        window.addGLEventListener(eventListener);

        final Animator animator = new Animator(window);
        animator.start();
        initializationFuture.join();
        window.invokeOnCurrentThread(() -> {
          try {
            statement.evaluate();
          } catch (Throwable throwable) {
            throwable.printStackTrace();
          }
        });
        testCompletionFuture.join();
        animator.stop();
      }
    };
  }
}
