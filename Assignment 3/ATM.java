import java.io.*;
import java.util.*;

/** Q1 - ATM withdrawal: try / multi-catch / finally, plus the error-code alternative. */
public class ATM {

    // Checked, user-defined: caller is forced to handle "not enough money"
    static class InsufficientFundsException extends Exception {
        InsufficientFundsException(String m) { super(m); }
    }

    // Reads the card-reader file. Lines look like:  1001 5000
    static double readBalance(String acc, String file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {   // FileNotFoundException (checked)
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.trim().split("\\s+");
                if (p.length == 2 && p[0].equals(acc)) return Double.parseDouble(p[1]); // NumberFormatException (unchecked)
            }
        }
        throw new IOException("Account not found: " + acc);
    }

    static void withdraw(String input, String cardFile) {
        System.out.println("INPUT \"" + input + "\"  (card file: " + cardFile + ")");
        Scanner sc = new Scanner(input);
        try {
            int acc = sc.nextInt();            // InputMismatchException (letters) / NoSuchElementException (missing)
            double amt = sc.nextDouble();
            if (amt <= 0) throw new IllegalArgumentException("Amount must be positive");   // unchecked
            double bal = readBalance(String.valueOf(acc), cardFile);                         // checked IOException
            if (amt > bal) throw new InsufficientFundsException("Balance " + bal + " is less than " + amt);
            System.out.println("  OK: dispensed " + amt + ", new balance " + (bal - amt));

        // Multi-catch: the alternatives must NOT be related by inheritance.
        // (InputMismatchException is a subclass of NoSuchElementException, so we catch the parent.)
        } catch (NoSuchElementException | IllegalArgumentException e) {
            System.out.println("  [multi-catch] Invalid input: " + e);
        } catch (InsufficientFundsException e) {
            System.out.println("  [InsufficientFunds] " + e.getMessage());
        } catch (FileNotFoundException e) {            // narrower  -> must come first
            System.out.println("  [FileNotFound] Card reader unavailable: " + e.getMessage());
        } catch (IOException e) {                      // broader parent -> after FileNotFoundException
            System.out.println("  [IOException] " + e.getMessage());
        } finally {
            sc.close();
            System.out.println("  [finally] card returned & scanner closed");
        }
    }

    // ---- Alternative: no exceptions, error codes + hasNextXxx() pre-checks ----
    static int withdrawCode(String input, double balance) {
        Scanner sc = new Scanner(input);
        if (!sc.hasNextInt())    return -1;   // bad account number
        sc.nextInt();
        if (!sc.hasNextDouble()) return -2;   // bad amount
        double amt = sc.nextDouble();
        if (amt <= 0)            return -3;   // negative / zero
        if (amt > balance)       return -4;   // insufficient funds
        return 0;                             // success (file-missing still needs its own check)
    }

    static void broaderCatchFirstDemo() {
        // Uncomment to see: "exception FileNotFoundException has already been caught"
        // try { new FileReader("x"); }
        // catch (IOException e) { }
        // catch (FileNotFoundException e) { }   // COMPILE ERROR - unreachable
    }

    static void uncaught() { int[] a = new int[2]; a[5] = 1; }   // for the stack-trace demo

    public static void main(String[] args) throws Exception {
        String card = "cards.txt";
        try (PrintWriter w = new PrintWriter(card)) { w.println("1001 5000"); w.println("1002 200"); }

        String[] tests = { "1001 1500", "abc 500", "1001 -50", "1002 900", "1001", "9999 100" };
        for (String t : tests) withdraw(t, card);
        withdraw("1001 100", "missing_reader.txt");      // card-reader file missing

        System.out.println("\nError-code version: " + withdrawCode("1001 -50", 5000) + ", "
                + withdrawCode("abc 5", 5000) + ", " + withdrawCode("1001 9000", 5000));

        if (args.length > 0) uncaught();   // run:  java ATM crash   -> prints the stack trace
    }
}
