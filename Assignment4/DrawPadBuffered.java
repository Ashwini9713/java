import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/* Alternative: draw each dot ONCE onto an off-screen image; paint() just copies the image.
   paint time: O(1) in clicks (one drawImage)      space: O(width x height) pixels, independent of n. */
public class DrawPadBuffered extends Frame {
    private final BufferedImage canvas = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_ARGB);
    private final Label status = new Label("Clicks: 0");
    private int count = 0;

    public DrawPadBuffered() {
        super("Draw Pad (buffered)");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Graphics2D g = canvas.createGraphics();
                g.setColor(Color.BLACK);
                g.fillOval(e.getX() - 3, e.getY() - 3, 6, 6);   // drawn once
                g.dispose();
                status.setText("Clicks: " + (++count));
                repaint();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); System.exit(0); }
        });
        setSize(400, 300);
    }
    @Override public void paint(Graphics g) { super.paint(g); g.drawImage(canvas, 0, 0, null); }
    public static void main(String[] args) { new DrawPadBuffered().setVisible(true); }
}
