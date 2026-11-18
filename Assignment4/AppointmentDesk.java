import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/* ===== TASK 2 - Hospital outpatient appointment desk =====
   Containment tree:
   JFrame -> contentPane(BorderLayout)
      NORTH  : JPanel(GridLayout 5x2) = JLabel+JTextField(patient ID), JLabel+JComboBox(department),
                                         JLabel+JComboBox(doctor), JLabel+JComboBox(time slot)
      CENTER : JPanel(FlowLayout)     = JButton(Book)
      SOUTH  : JLabel(status)                                                                      */
public class AppointmentDesk extends JFrame {

    /** Doctor lookup seam. Map version = O(1) per change; DB version = a query per change. */
    interface DoctorSource { List<String> doctorsFor(String dept) throws Exception; }

    static class MapDoctorSource implements DoctorSource {           // loaded ONCE
        private final Map<String, List<String>> map = new LinkedHashMap<>();
        MapDoctorSource() {
            map.put("Cardiology",  List.of("Dr. Rao", "Dr. Iyer"));
            map.put("Orthopedics", List.of("Dr. Khan", "Dr. Mehta", "Dr. Das"));
            map.put("Pediatrics",  List.of("Dr. Nair"));
        }
        Set<String> departments() { return map.keySet(); }
        @Override public List<String> doctorsFor(String dept) { return map.getOrDefault(dept, List.of()); }
    }

    static class DbDoctorSource implements DoctorSource {            // alternative: query every time
        private final String url, user, pwd;
        DbDoctorSource(String url, String user, String pwd) { this.url = url; this.user = user; this.pwd = pwd; }
        @Override public List<String> doctorsFor(String dept) throws SQLException {
            List<String> out = new ArrayList<>();
            try (Connection c = DriverManager.getConnection(url, user, pwd);
                 PreparedStatement ps = c.prepareStatement("SELECT name FROM doctors WHERE dept = ?")) {
                ps.setString(1, dept);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getString(1)); }
            }
            return out;                                              // network + query cost on EVERY change
        }
    }

    private static final String SELECT = "-- select --";
    private final MapDoctorSource source = new MapDoctorSource();
    private final Set<String> bookedSlots = new HashSet<>();         // "doctor|slot"
    private final JTextField patientId = new JTextField(12);
    private final JComboBox<String> dept = new JComboBox<>();
    private final JComboBox<String> doctor = new JComboBox<>();
    private final JComboBox<String> slot = new JComboBox<>(
            new String[]{SELECT, "09:00", "09:30", "10:00", "10:30", "11:00"});
    private final JButton book = new JButton("Book appointment");
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private boolean bookingInProgress = false;

    public AppointmentDesk() {
        super("Outpatient Appointment Desk");
        dept.addItem(SELECT);
        for (String d : source.departments()) dept.addItem(d);
        doctor.addItem(SELECT);

        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Patient ID:")); form.add(patientId);
        form.add(new JLabel("Department:")); form.add(dept);
        form.add(new JLabel("Doctor:"));     form.add(doctor);
        form.add(new JLabel("Time slot:"));  form.add(slot);
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(book);
        add(form, BorderLayout.NORTH); add(buttons, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);

        // Event 1: ItemEvent/ActionEvent from the department list -> refresh doctors
        dept.addActionListener(e -> refreshDoctors());
        // Event 2: ActionEvent from the Book button
        book.addActionListener(e -> bookAppointment());
        // Event 3: DocumentEvent from the patient ID field -> a booking is "in progress"
        patientId.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { bookingInProgress = true; }
            public void removeUpdate(DocumentEvent e)  { bookingInProgress = !patientId.getText().isEmpty(); }
            public void changedUpdate(DocumentEvent e) { }
        });
        // Event 4: WindowEvents - print order for the trace task, confirm before closing
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e)      { System.out.println("windowOpened"); }
            @Override public void windowActivated(WindowEvent e)   { System.out.println("windowActivated"); }
            @Override public void windowDeactivated(WindowEvent e) { System.out.println("windowDeactivated"); }
            @Override public void windowIconified(WindowEvent e)   { System.out.println("windowIconified"); }
            @Override public void windowDeiconified(WindowEvent e) { System.out.println("windowDeiconified"); }
            @Override public void windowClosing(WindowEvent e) {
                System.out.println("windowClosing");
                if (bookingInProgress) {
                    int r = JOptionPane.showConfirmDialog(AppointmentDesk.this,
                            "A booking is in progress. Close anyway?", "Confirm close", JOptionPane.YES_NO_OPTION);
                    if (r != JOptionPane.YES_OPTION) return;
                }
                dispose();
            }
            @Override public void windowClosed(WindowEvent e)      { System.out.println("windowClosed"); System.exit(0); }
        });
        pack(); setLocationRelativeTo(null);
    }

    // (a) refresh the doctor list when the department changes
    private void refreshDoctors() {
        doctor.removeAllItems();
        doctor.addItem(SELECT);
        String d = (String) dept.getSelectedItem();
        if (d != null && !d.equals(SELECT)) {
            try { for (String name : source.doctorsFor(d)) doctor.addItem(name); }
            catch (Exception ex) { status.setText("Could not load doctors: " + ex.getMessage()); }
        }
        bookingInProgress = true;
    }

    // (b) validate and book
    private void bookAppointment() {
        String id = patientId.getText().trim();
        String d = (String) dept.getSelectedItem(), doc = (String) doctor.getSelectedItem(), s = (String) slot.getSelectedItem();
        if (!id.matches("\\d{4,10}"))            { error("Patient ID must be 4-10 digits"); return; }
        if (d == null || d.equals(SELECT))       { error("Select a department"); return; }
        if (doc == null || doc.equals(SELECT))   { error("Select a doctor"); return; }
        if (s == null || s.equals(SELECT))       { error("Select a time slot"); return; }
        if (!bookedSlots.add(doc + "|" + s))     { error(doc + " is already booked at " + s); return; }
        status.setForeground(new Color(0, 120, 0));
        status.setText("Confirmed: patient " + id + " with " + doc + " at " + s);
        bookingInProgress = false;
        patientId.setText(""); dept.setSelectedIndex(0);
    }
    private void error(String m) { status.setForeground(Color.RED); status.setText(m); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppointmentDesk().setVisible(true));
    }
}
