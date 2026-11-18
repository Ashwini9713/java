import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/* ===== TASK 1 - Redesigned fee-payment window =====
   Layers:  FeeWindow (view: collects input, shows results)
            -> FeeService (validation + payment rules)
               -> FeeRepository (interface) <- JdbcFeeRepository (database work)               */

class ValidationException extends Exception {
    ValidationException(String m) { super(m); }
}

interface FeeRepository {                                  // seam that makes FeeService testable
    void save(String regNo, double amount) throws SQLException;
}

class JdbcFeeRepository implements FeeRepository {
    private final String url, user, pwd;
    JdbcFeeRepository(String url, String user, String pwd) { this.url = url; this.user = user; this.pwd = pwd; }
    @Override public void save(String regNo, double amount) throws SQLException {
        // try-with-resources closes the connection and statement even on failure;
        // PreparedStatement prevents SQL injection
        try (Connection c = DriverManager.getConnection(url, user, pwd);
             PreparedStatement ps = c.prepareStatement("INSERT INTO fees(reg_no, amount) VALUES (?, ?)")) {
            ps.setString(1, regNo);
            ps.setDouble(2, amount);
            ps.executeUpdate();
        }
    }
}

class FeeService {
    private final FeeRepository repo;
    private String lastReceipt;                            // used by the Receipt button
    FeeService(FeeRepository repo) { this.repo = repo; }

    /** Pure logic: no GUI, no DB -> unit-testable. Returns the parsed amount. */
    static double validate(String regNo, String amountText) throws ValidationException {
        if (regNo == null || regNo.trim().isEmpty()) throw new ValidationException("Register number is required");
        if (!regNo.trim().matches("[A-Za-z0-9]{6,12}")) throw new ValidationException("Register number must be 6-12 letters/digits");
        if (amountText == null || amountText.trim().isEmpty()) throw new ValidationException("Amount is required");
        double a;
        try { a = Double.parseDouble(amountText.trim()); }
        catch (NumberFormatException e) { throw new ValidationException("Amount must be a number"); }
        if (a <= 0) throw new ValidationException("Amount must be greater than zero");
        return a;
    }

    /** Slow: call from a background thread, never from the event-dispatch thread. */
    String pay(String regNo, String amountText) throws ValidationException, SQLException, InterruptedException {
        double a = validate(regNo, amountText);
        Thread.sleep(3000);                                // simulated bank confirmation
        repo.save(regNo.trim(), a);
        lastReceipt = "RCPT-" + System.currentTimeMillis() + " | " + regNo.trim() + " | Rs " + a;
        return lastReceipt;
    }
    String lastReceipt() { return lastReceipt; }
}

public class FeeWindow extends Frame {
    private final TextField regNo = new TextField(15), amount = new TextField(10);
    private final Button pay = new Button("Pay"), clear = new Button("Clear"), receipt = new Button("Receipt");
    private final Label status = new Label("Ready");
    private final FeeService service;

    FeeWindow(FeeService service) {
        super("Fee Payment");
        this.service = service;
        setLayout(new GridLayout(4, 2, 5, 5));
        add(new Label("Register No:")); add(regNo);
        add(new Label("Amount:"));      add(amount);
        add(pay);                       add(clear);
        add(receipt);                   add(status);

        // one small listener per button (no if-else chain, no string comparison)
        pay.addActionListener(e -> onPay());
        clear.addActionListener(e -> { regNo.setText(""); amount.setText(""); status.setText("Cleared"); });
        receipt.addActionListener(e -> {
            String r = service.lastReceipt();
            status.setText(r == null ? "No payment made yet" : r);
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); System.exit(0); }
        });
        setSize(420, 180);
    }

    private void onPay() {                                 // runs on the event-dispatch thread
        final String r = regNo.getText(), a = amount.getText();
        try { FeeService.validate(r, a); }                 // cheap -> fine on the EDT
        catch (ValidationException ex) { status.setText(ex.getMessage()); return; }

        pay.setEnabled(false);
        status.setText("Contacting bank...");
        new Thread(() -> {                                 // slow work off the EDT: window stays responsive
            String msg;
            try { msg = "Paid: " + service.pay(r, a); }
            catch (ValidationException | SQLException | InterruptedException ex) { msg = "Payment failed: " + ex.getMessage(); }
            final String m = msg;
            EventQueue.invokeLater(() -> { status.setText(m); pay.setEnabled(true); });
        }, "payment-worker").start();
    }

    public static void main(String[] args) {
        String url = System.getProperty("db.url");
        FeeRepository repo = (url != null)
                ? new JdbcFeeRepository(url, System.getProperty("db.user", "root"), System.getProperty("db.pwd", ""))
                : (reg, amt) -> System.out.println("[in-memory] saved " + reg + " " + amt);
        EventQueue.invokeLater(() -> new FeeWindow(new FeeService(repo)).setVisible(true));
    }
}
