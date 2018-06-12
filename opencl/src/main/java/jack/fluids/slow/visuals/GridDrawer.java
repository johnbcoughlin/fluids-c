package jack.fluids.slow.visuals;

import jack.fluids.slow.Grid;
import jack.fluids.slow.Neighborhood;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

public class GridDrawer {
  public static final int H = 600;
  public static final int W = 600;

  public static void drawGrid(Grid grid) {
    JFrame frame = new MyFrame(grid);
    Listener listener = new Listener(frame);
    frame.addWindowListener(listener);
    frame.addKeyListener(listener);

    frame.setVisible(true);
  }

  private static class MyFrame extends JFrame {
    public MyFrame(Grid grid) {
      setTitle("Grid");
      setSize(W, H);
      add(new GridPane(grid));
    }
  }

  private static class GridPane extends JLayeredPane {
    private final Grid grid;
    private final AffineTransform tx;

    public GridPane(Grid grid) {
      super();
      this.grid = grid;
      tx = AffineTransform.getScaleInstance(
          GridDrawer.W / (grid.nx() * grid.dx() + grid.dx()),
          -GridDrawer.H / (grid.ny() * grid.dy() + grid.dy()));
      tx.translate(grid.dx() / 2, -grid.ny() * grid.dy() - grid.dy() / 2);
      init();
    }

    private void init() {
      for (int i = 0; i < grid.nx(); i++) {
        for (int j = 0; j < grid.ny(); j++) {
          final int finalI = i;
          final int finalJ = j;
          grid.uNeighborhood(i, j).ifPresent(nb ->
              add(new UCellPanel(finalI, finalJ, nb, tx)));
        }
      }
      setSize(W, H);
    }

    @Override
    public void paint(Graphics graphics) {
      super.paint(graphics);
      Graphics2D g = (Graphics2D) graphics;
      g.setTransform(tx);

      g.setColor(Color.BLUE);
      g.setStroke(new BasicStroke(0.04f));

      grid.mesh.segments().forEach(segment -> {
        g.draw(new Line2D.Double(
            segment.a().x(), segment.a().y(),
            segment.b().x(), segment.b().y()
        ));
      });
    }
  }

  private static class UCellPanel extends JComponent implements MouseListener {
    private final Neighborhood neighborhood;
    private final AffineTransform tx;
    private final int i;
    private final int j;

    private UCellPanel(int i, int j, Neighborhood neighborhood, AffineTransform tx) {
      this.i = i;
      this.j = j;
      this.neighborhood = neighborhood;
      this.tx = tx;
      this.setBounds(0, 0, W, H);
      this.setOpaque(false);
    }

    @Override
    public void paint(Graphics graphics) {
      Graphics2D g = ((Graphics2D) graphics);
      g.setTransform(tx);

      g.setColor(Color.RED);
      g.draw(new Ellipse2D.Double(neighborhood.p().x(), neighborhood.p().y(), 0.04, 0.04));
      super.paint(graphics);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
      setVisible(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setVisible(false);
    }
  }

  private static class Listener implements WindowListener, KeyListener {
    private final Frame frame;

    private Listener(Frame frame) {this.frame = frame;}

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
      frame.setVisible(false);
      System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
      if ((e.getKeyChar() == 'q' || e.getKeyChar() == 'w') && e.isMetaDown()) {
        frame.setVisible(false);
        System.exit(0);
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
  }
}
