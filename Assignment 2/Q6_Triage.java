import java.util.*;

/** Q6: Priority queue triage with arrival tie-break, ageing (anti-starvation) and benchmark. */
public class Q6_Triage {
    private long counter = 0;
    // TODO 1
    private final PriorityQueue<Patient> queue = new PriorityQueue<>(
            Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));
    void admit(String name, int severity) { queue.add(new Patient(name, severity, counter++)); }
    Patient next() { return queue.poll(); }

    /** Fair rule: every AGE_STEP admissions of waiting lowers the severity number by 1 (min 1). */
    static class FairTriage {
        static final long AGE_STEP = 100; long clock = 0;
        final PriorityQueue<Patient> pq;
        FairTriage() { pq = new PriorityQueue<>(Comparator.comparingLong((Patient p) -> effective(p)).thenComparingLong(p -> p.arrival)); }
        long effective(Patient p) { long boost = (clock - p.arrival) / AGE_STEP; return Math.max(1, p.severity - boost); }
        // NOTE: effective priority changes with the clock, so we rebuild the heap periodically (O(n)) instead of per operation.
        void admit(String n, int s) { pq.add(new Patient(n, s, clock++)); if (clock % AGE_STEP == 0) { List<Patient> t = new ArrayList<>(pq); pq.clear(); pq.addAll(t); } }
        Patient next() { return pq.poll(); }
    }

    public static void main(String[] args) {
        triageHand();
        // growth benchmark
        System.out.printf("%n%-10s %14s %14s%n", "n", "add ns/op", "poll ns/op");
        for (int n : new int[]{1_000, 10_000, 100_000, 1_000_000}) {
            double add = 0, poll = 0;
            for (int rep = 0; rep < 6; rep++) {
                PriorityQueue<Patient> q = new PriorityQueue<>(Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));
                Random r = new Random(5);
                long t0 = System.nanoTime(); for (int i = 0; i < n; i++) q.add(new Patient("P", 1 + r.nextInt(5), i));
                long t1 = System.nanoTime(); while (q.poll() != null) { }
                long t2 = System.nanoTime();
                if (rep > 1) { add += (t1 - t0) / (double) n / 4; poll += (t2 - t1) / (double) n / 4; } // first 2 reps = JIT warm-up
            }
            System.out.printf("%-10d %14.1f %14.1f%n", n, add, poll);
        }
        // starvation demo
        Q6_Triage er = new Q6_Triage(); er.admit("Minor", 5);
        for (int i = 0; i < 1000; i++) { er.admit("Crit" + i, 1); er.next(); }
        System.out.println("\nStarvation (plain): after 1000 critical arrivals, 'Minor' is still waiting; queue size=" + er.queue.size());
        FairTriage ft = new FairTriage(); ft.admit("Minor", 5);
        String servedAt = "never";
        for (int i = 0; i < 1000; i++) { ft.admit("Crit" + i, 1); Patient p = ft.next(); if (p.name.equals("Minor")) { servedAt = "after " + i + " arrivals"; break; } }
        System.out.println("Starvation (ageing rule): Minor served " + servedAt);
        // 5000 patients/day
        Q6_Triage day = new Q6_Triage(); Random r = new Random(9);
        long t0 = System.nanoTime(); for (int i = 0; i < 5000; i++) day.admit("P" + i, 1 + r.nextInt(5)); while (day.next() != null) { }
        System.out.printf("5,000 patients admit+serve whole day: %.2f ms%n", (System.nanoTime() - t0) / 1e6);
    }
    static void triageHand() {
        Q6_Triage er = new Q6_Triage();
        er.admit("Ravi", 3); er.admit("Meena", 1); er.admit("John", 3); er.admit("Asha", 5); er.admit("Kumar", 1); er.admit("Lata", 2);
        System.out.println("Raw PriorityQueue print (heap order, NOT treatment order): " + er.queue);
        StringBuilder order = new StringBuilder(); Patient p;
        while ((p = er.next()) != null) order.append(p).append(' ');
        System.out.println(order.toString().trim());
        Q6_Triage e8 = new Q6_Triage();
        String[] n = {"P1","P2","P3","P4","P5","P6","P7","P8"}; int[] s = {4,2,5,2,1,4,1,3};
        for (int i = 0; i < 8; i++) e8.admit(n[i], s[i]);
        StringBuilder o = new StringBuilder(); while ((p = e8.next()) != null) o.append(p).append(' ');
        System.out.println("8-patient check (hand answer: P5 P7 P2 P4 P8 P1 P6 P3): " + o.toString().trim());
    }
}

class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name, int severity, long arrival) { this.name = name; this.severity = severity; this.arrival = arrival; }
    @Override public String toString() { return name + "(S" + severity + ")"; }
}
