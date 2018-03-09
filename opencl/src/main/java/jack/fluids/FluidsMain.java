package jack.fluids;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;

public class FluidsMain {
  public static void main(String[] args) {
    GLProfile profile = GLProfile.get(GLProfile.GL4);
    System.out.println(profile);
    GLCapabilities capabilities = new GLCapabilities(profile);
    GLWindow window = GLWindow.create(capabilities);

    window.setTitle("a triangle");
    window.setSize(200, 200);
    window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
    window.setVisible(true);

    FluidsApp fluidsApp = new FluidsApp();
    window.addGLEventListener(fluidsApp);
    window.addKeyListener(fluidsApp);

    final Animator animator = new Animator(window);
    animator.start();

    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowDestroyed(WindowEvent e) {
        animator.stop();
        System.exit(0);
      }
    });
  }
}
