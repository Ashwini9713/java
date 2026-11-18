import java.util.*;

/** TASK 2 - Data representation, casting risks, input, and array alternatives. */
public class Task2 {
    // ---- Chosen types ----
    // slot number     : int        (small whole number, used as array index)
    // vehicle number  : String     (letters + digits, e.g. TN01AB1111)
    // entry time      : int        (minutes since midnight; whole minutes, easy to subtract)
    // duration        : int        (minutes)
    // fee             : double     (rupees; BigDecimal/long paise would be used for real money)
    // floors/slots    : String[][] jagged (floors have different slot counts, no wasted cells)
    static final double RATE_PER_HOUR = 30;

    static double wrongFeeIntDivision(int minutes) {      // BUG: 130 / 60 = 2 (integer division) -> 2 h
        int hours = minutes / 60;
        return hours * RATE_PER_HOUR;
    }
    static double wrongFeeCastAfter(int minutes) {        // BUG: cast too late, division already truncated
        double hours = (double) (minutes / 60);
        return Math.ceil(hours) * RATE_PER_HOUR;
    }
    static double wrongFeeCastTruncates(int minutes) {    // BUG: (int) truncates 2.17 -> 2
        return (int) (minutes / 60.0) * RATE_PER_HOUR;
    }
    static double correctFee(int minutes) {               // FIX: integer ceiling, no floating point at all
        int hours = (minutes + 59) / 60;
        return hours * RATE_PER_HOUR;
    }

    public static void main(String[] args) {
        System.out.println("=== Casting / integer-division risk (130 minutes, Rs 30/hour; correct = 3 h = 90) ===");
        System.out.println("minutes / 60            -> " + wrongFeeIntDivision(130));
        System.out.println("(double)(minutes / 60)  -> " + wrongFeeCastAfter(130));
        System.out.println("(int)(minutes / 60.0)   -> " + wrongFeeCastTruncates(130));
        System.out.println("(minutes + 59) / 60     -> " + correctFee(130) + "   <- design used");
        System.out.println("(int) 2.9 = " + (int) 2.9 + ",  (int) -2.9 = " + (int) -2.9 + ",  (byte) 200 = " + (byte) 200
                + ",  0.1 + 0.2 == 0.3 ? " + (0.1 + 0.2 == 0.3));

        System.out.println("\n=== Reading input (Scanner) ===");
        // Real run: Scanner in = new Scanner(System.in);  Here a String stands in for the keyboard.
        Scanner in = new Scanner("TN01AB1111 540 670");
        String reg = in.next(); int entry = in.nextInt(); int exit = in.nextInt();
        System.out.println(reg + " stayed " + (exit - entry) + " min -> Rs " + correctFee(exit - entry));
        // Alternative: command-line arguments  ->  int entry = Integer.parseInt(args[1]);  (fixed order, no prompts)

        System.out.println("\n=== Floors and slots: three representations (floors with 3, 2 and 4 slots) ===");
        int[] slotsPerFloor = {3, 2, 4};

        // (1) Jagged array - used in the design
        String[][] jagged = new String[3][];
        for (int f = 0; f < 3; f++) jagged[f] = new String[slotsPerFloor[f]];
        jagged[2][3] = "TN09Z9";
        System.out.println("Jagged        : cells = " + Arrays.stream(jagged).mapToInt(r -> r.length).sum() + " (no waste), lookup jagged[2][3] = O(1)");

        // (2) 1-D array, index = floor * slotsPerFloor + slot  -> needs a FIXED S, so S = max = 4
        int S = 4; String[] flat = new String[3 * S];
        flat[2 * S + 3] = "TN09Z9";
        System.out.println("1-D (f*S+s)   : cells = " + flat.length + " (3 unused), lookup O(1), but slot 3 on floor 1 would be a bug");
        // Variable floors: store prefix offsets instead of the simple formula
        int[] offset = new int[4]; for (int f = 0; f < 3; f++) offset[f + 1] = offset[f] + slotsPerFloor[f];
        String[] packed = new String[offset[3]];
        packed[offset[2] + 3] = "TN09Z9";
        System.out.println("1-D + offsets : cells = " + packed.length + ", lookup O(1) = offset[f] + s, extra int[F+1]");

        // (3) ArrayList<String[]>
        List<String[]> rows = new ArrayList<>();
        for (int s : slotsPerFloor) rows.add(new String[s]);
        rows.get(2)[3] = "TN09Z9";
        System.out.println("ArrayList<String[]> : cells = 9, lookup O(1) (get + index), floors can be ADDED at run time");
    }
}
