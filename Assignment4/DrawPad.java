import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/* ===== TASK 4 - DrawPad (repaired; TODO 1 and TODO 2 completed) =====
   Original defects:
   1. Method was spelled 'mouseclicked' (lower-case c): it OVERLOADS nothing and never overrides
      MouseAdapter.mouseClicked, so Java never calls it -> count stays 0, no dot.
   2. Drawing with getGraphics() is temporary: when the window is minimised/restored AWT calls
      paint(), which knows nothing about the dots, so they vanish.
   Prevention: @Override on  mouseclicked  gives the compile error
      "method does not override or implement a method from a supertype".                         */
public class DrawPad extends Frame {
    private final List<Point> points = new ArrayList<>();
    private final Label status = new Label("Clicks: 0");

    public DrawPad() {
        super("Draw Pad");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            // TODO 1 (done)
            @Override public void mouseClicked(MouseEvent e) {
                points.add(e.getPoint());
                status.setText("Clicks: " + points.size());
                repaint();                       // ask AWT to call paint() again
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
            @Override public void windowClosed(WindowEvent e)  { System.exit(0); }
        });
        setSize(400, 300);
    }

    @Override public void paint(Graphics g) {
        super.paint(g);                          // let the frame paint its components
        // TODO 2 (done): 6x6 circle CENTRED on each point
        for (Point p : points) g.fillOval(p.x - 3, p.y - 3, 6, 6);
    }

    public static void main(String[] args) { new DrawPad().setVisible(true); }
}
