import java.util.*;

/*
 * TASK 3 - PSEUDOCODE (generic form - re-format to the SIMATS pseudocode standard in your handout)
 *
 * ALGORITHM Allocate(vehicleNo)
 *   FOR floor <- 0 TO floors-1 DO
 *     FOR slot <- 0 TO slotsOnFloor(floor)-1 DO
 *       IF slots[floor][slot] = NULL THEN
 *         slots[floor][slot] <- vehicleNo
 *         RETURN (floor, slot)            // stop at the FIRST free slot
 *       END IF
 *     END FOR
 *   END FOR
 *   RETURN NULL                           // facility full
 * END ALGORITHM
 *
 * ALGORITHM ComputeFee(minutes)
 *   IF minutes <= 0 THEN RETURN 0
 *   hours <- (minutes + 59) DIV 60
 *   IF hours = 1 THEN fee <- 30
 *   ELSE fee <- 30 + (hours - 1) * 20
 *   IF fee > 200 THEN fee <- 200          // daily maximum
 *   RETURN fee
 * END ALGORITHM
 */
public class ParkingFacility {
    // slots[floor][slot] holds a vehicle number, or null if the slot is free
    private final String[][] slots;
    public ParkingFacility(int[] slotsPerFloor) {
        slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            slots[f] = new String[slotsPerFloor[f]];
        }
    }
    /** Returns {floor, slot} of the first free slot, or null if it is full. */
    public int[] allocate(String vehicleNo) {
        for (int f = 0; f < slots.length; f++) {
            for (int s = 0; s < slots[f].length; s++) {
                if (slots[f][s] == null) {
                    slots[f][s] = vehicleNo;
                    return new int[]{f, s};          // return leaves both loops at once
                }
            }
        }
        return null;                                  // no free slot anywhere
    }
    /** Frees a slot. Returns false if the position is invalid or already free. */
    public boolean release(int floor, int slot) {
        if (floor < 0 || floor >= slots.length) return false;
        if (slot < 0 || slot >= slots[floor].length) return false;
        if (slots[floor][slot] == null) return false;
        slots[floor][slot] = null;
        return true;
    }
    /** Fee slabs with if-else; every started hour is charged. */
    static double computeFee(int minutes) {
        if (minutes <= 0) return 0;
        int hours = (minutes + 59) / 60;
        double fee;
        if (hours == 1) fee = 30;
        else fee = 30 + (hours - 1) * 20;
        if (fee > 200) fee = 200;
        return fee;
    }
    public static void main(String[] args) {
        // floor 0 has 2 slots, floor 1 has 1 slot
        ParkingFacility p = new ParkingFacility(new int[]{2, 1});
        System.out.println(java.util.Arrays.toString(p.allocate("TN01AB1111")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN02CD2222")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN03EF3333")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN04GH4444")));
        System.out.println(p.release(0, 1) + " " + p.release(0, 1) + " "
                + p.release(5, 0));
        System.out.println(java.util.Arrays.toString(p.allocate("TN04GH4444")));
    }
}

/** Alternative (run separately): free-count per floor + stack of free slot numbers. */
class FastFacility {
    private final String[][] slots;
    private final int[] freeCount;
    private final ArrayDeque<Integer>[] freeStack;
    @SuppressWarnings("unchecked")
    FastFacility(int[] slotsPerFloor) {
        int F = slotsPerFloor.length;
        slots = new String[F][]; freeCount = new int[F]; freeStack = new ArrayDeque[F];
        for (int f = 0; f < F; f++) {
            slots[f] = new String[slotsPerFloor[f]];
            freeCount[f] = slotsPerFloor[f];
            freeStack[f] = new ArrayDeque<>();
            for (int s = slotsPerFloor[f] - 1; s >= 0; s--) freeStack[f].push(s);   // lowest slot on top
        }
    }
    int[] allocate(String v) {                                   // O(F): find a floor with space, then O(1) pop
        for (int f = 0; f < slots.length; f++) {
            if (freeCount[f] == 0) continue;                     // skip a full floor without scanning its slots
            int s = freeStack[f].pop(); freeCount[f]--;
            slots[f][s] = v;
            return new int[]{f, s};
        }
        return null;
    }
    boolean release(int f, int s) {                              // O(1)
        if (f < 0 || f >= slots.length || s < 0 || s >= slots[f].length || slots[f][s] == null) return false;
        slots[f][s] = null; freeCount[f]++; freeStack[f].push(s);
        return true;
    }
}

class Task3Extras {
    public static void main(String[] args) {
        System.out.println("--- Fee slabs (first hour 30, then 20/h, daily max 200) ---");
        for (int m : new int[]{0, 1, 60, 61, 130, 600, 1440})
            System.out.println(m + " min -> Rs " + ParkingFacility.computeFee(m));
        System.out.println("\n--- Alternative: free-count + stack (same expected output) ---");
        FastFacility ff = new FastFacility(new int[]{2, 1});
        System.out.println(Arrays.toString(ff.allocate("A")));
        System.out.println(Arrays.toString(ff.allocate("B")));
        System.out.println(Arrays.toString(ff.allocate("C")));
        System.out.println(Arrays.toString(ff.allocate("D")));
        System.out.println(ff.release(0, 1) + " " + ff.release(0, 1) + " " + ff.release(5, 0));
        System.out.println(Arrays.toString(ff.allocate("D")));
    }
}
