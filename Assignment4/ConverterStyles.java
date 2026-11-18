import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.event.*;

/* ===== TASK 5.1 / 5.2 - the four styles (after the change: Enter key + dialog box) =====
   Run:  java ConverterStyles a|b|c|d|e      (e = shared Action object)

   What changed for 5.2 (see the commented lines marked CHANGE):
   a) implements ActionListener : 1 line (amount.addActionListener(this)) + dialog line; getSource() not needed
   b) named inner class         : 1 line (reuse the same listener object) + dialog line
   c) anonymous inner class     : the whole anonymous class had to be copied/stored in a variable to reuse -> most edits
   d) lambda                    : 1 line (reuse the lambda variable) + dialog line                               */
public class ConverterStyles {

    static double rate(String c) { return c.equals("USD") ? 0.012 : 0.011; }
    /** shared pure logic: returns formatted text or null if the amount is invalid */
    static String convertText(String amountText, String cur) {
        try { return String.format("%.2f %s", Double.parseDouble(amountText.trim()) * rate(cur), cur); }
        catch (NumberFormatException e) { return null; }
    }

    /** Common screen; subclasses only differ in how the listener is written. */
    static abstract class Base extends JFrame {
        final JTextField amount = new JTextField(10);
        final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
        final JButton convert = new JButton("Convert");
        final JLabel result = new JLabel(" ");
        Base(String title) {
            super(title);
            setLayout(new FlowLayout());
            add(new JLabel("INR")); add(amount); add(target); add(convert); add(result);
            // window closing handled with a WindowAdapter in every style
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { dispose(); System.exit(0); }
            });
        }
        void show(String text) {                             // CHANGE: result now also goes to a dialog
            result.setText(text == null ? "Invalid amount" : text);
            JOptionPane.showMessageDialog(this, text == null ? "Invalid amount" : text);
        }
        String compute() { return convertText(amount.getText(), (String) target.getSelectedItem()); }
    }

    // (a) the frame itself implements ActionListener
    static class StyleA extends Base implements ActionListener {
        StyleA() { super("A: frame implements ActionListener");
            convert.addActionListener(this);
            amount.addActionListener(this);                  // CHANGE (Enter key)
            pack(); }
        @Override public void actionPerformed(ActionEvent e) { show(compute()); }
    }

    // (b) named inner class
    static class StyleB extends Base {
        class ConvertListener implements ActionListener {
            @Override public void actionPerformed(ActionEvent e) { show(compute()); }
        }
        StyleB() { super("B: named inner class");
            ConvertListener l = new ConvertListener();
            convert.addActionListener(l);
            amount.addActionListener(l);                     // CHANGE (Enter key)
            pack(); }
    }

    // (c) anonymous inner class
    static class StyleC extends Base {
        StyleC() { super("C: anonymous inner class");
            ActionListener l = new ActionListener() {        // CHANGE: had to be stored in a variable to reuse it
                @Override public void actionPerformed(ActionEvent e) { show(compute()); }
            };
            convert.addActionListener(l);
            amount.addActionListener(l);
            pack(); }
    }

    // (d) lambda
    static class StyleD extends Base {
        StyleD() { super("D: lambda");
            ActionListener l = e -> show(compute());
            convert.addActionListener(l);
            amount.addActionListener(l);                     // CHANGE (Enter key)
            pack(); }
    }

    // (e) alternative: ONE Action object usable by button, menu item, keyboard shortcut
    static class StyleE extends Base {
        StyleE() { super("E: shared Action");
            Action convertAction = new AbstractAction("Convert") {
                @Override public void actionPerformed(ActionEvent e) { show(compute()); }
            };
            convertAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control ENTER"));
            convert.setAction(convertAction);                // button text + handler come from the Action
            amount.addActionListener(convertAction);
            JMenuBar mb = new JMenuBar(); JMenu m = new JMenu("Tools");
            m.add(new JMenuItem(convertAction));             // a new trigger = ONE line
            mb.add(m); setJMenuBar(mb);
            pack(); }
    }

    public static void main(String[] args) {
        String s = args.length > 0 ? args[0] : "d";
        SwingUtilities.invokeLater(() -> {
            JFrame f;
            switch (s) { case "a": f = new StyleA(); break; case "b": f = new StyleB(); break;
                         case "c": f = new StyleC(); break; case "e": f = new StyleE(); break;
                         default:  f = new StyleD(); }
            f.setVisible(true);
        });
    }
}
