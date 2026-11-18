import java.util.*;

/** Q2: ArrayList vs LinkedList vs ArrayDeque for playlist operations. */
public class Q2_ListBench {
    static volatile long sink;
    static long ms(Runnable r) { long t = System.nanoTime(); r.run(); return (System.nanoTime() - t) / 1_000_000; }
    static List<Integer> fill(List<Integer> l, int n) { for (int i = 0; i < n; i++) l.add(i); return l; }

    public static void main(String[] args) {
        int[] sizes = {10_000, 50_000, 200_000};
        System.out.printf("%-34s %8s %12s %12s%n", "Operation (time ms)", "n", "ArrayList", "LinkedList");
        for (int n : sizes) {
            int ops = Math.min(n, 20_000);
            Random rnd = new Random(1);
            int[] idx = new int[ops]; for (int i = 0; i < ops; i++) idx[i] = rnd.nextInt(n);
            Runnable[][] tests = new Runnable[5][2];
            String[] names = {"add at end (n adds)", "insert at front (ops)", "insert middle + remove middle (ops)", "get(k) random (ops)", "iterate whole list (for-each)"};
            long[][] r = new long[5][2];
            for (int k = 0; k < 2; k++) {
                for (int rep = 0; rep < 2; rep++) { // rep 0 = warm-up
                    List<Integer> l = k == 0 ? new ArrayList<>() : new LinkedList<>();
                    long a = ms(() -> { for (int i = 0; i < n; i++) l.add(i); });
                    List<Integer> L = l;
                    long b = ms(() -> { for (int i = 0; i < ops; i++) L.add(0, i); });
                    long c = ms(() -> { for (int i = 0; i < ops / 4; i++) { int m = L.size() / 2; L.add(m, i); L.remove(m); } });
                    long d = ms(() -> { long s = 0; for (int i = 0; i < ops; i++) s += L.get(idx[i]); sink = s; });
                    long e = ms(() -> { long s = 0; for (int x : L) s += x; sink = s; });
                    if (rep == 1) { r[0][k] = a; r[1][k] = b; r[2][k] = c; r[3][k] = d; r[4][k] = e; }
                }
            }
            for (int t = 0; t < 5; t++) System.out.printf("%-34s %8d %12d %12d%n", names[t], n, r[t][0], r[t][1]);
            System.out.println();
        }

        // Up Next queue: add at both ends, remove from front
        System.out.printf("%n%-34s %8s %12s %12s %12s%n", "Up Next: addLast/addFirst/pollFirst", "ops", "ArrayList", "LinkedList", "ArrayDeque");
        for (int n : new int[]{20_000, 100_000, 400_000}) {
            long[] t = new long[3];
            for (int rep = 0; rep < 2; rep++) {
                List<Integer> al = new ArrayList<>(); LinkedList<Integer> ll = new LinkedList<>(); ArrayDeque<Integer> ad = new ArrayDeque<>();
                long a = ms(() -> { for (int i = 0; i < n; i++) { if ((i & 1) == 0) al.add(i); else al.add(0, i); } for (int i = 0; i < n / 2; i++) al.remove(0); });
                long b = ms(() -> { for (int i = 0; i < n; i++) { if ((i & 1) == 0) ll.addLast(i); else ll.addFirst(i); } for (int i = 0; i < n / 2; i++) ll.pollFirst(); });
                long c = ms(() -> { for (int i = 0; i < n; i++) { if ((i & 1) == 0) ad.addLast(i); else ad.addFirst(i); } for (int i = 0; i < n / 2; i++) ad.pollFirst(); });
                if (rep == 1) t = new long[]{a, b, c};
            }
            System.out.printf("%-34s %8d %12d %12d %12d%n", "", n, t[0], t[1], t[2]);
        }
    }
}
