/** Q2 alternative: plain array with manual shifting (what ArrayList does internally). */
public class Q2_ManualArray {
    private String[] a = new String[4]; private int size;
    void add(String s) { if (size == a.length) a = java.util.Arrays.copyOf(a, size * 2); a[size++] = s; }          // amortised O(1)
    void insert(int i, String s) {                                                                                   // O(n) shifting
        if (size == a.length) a = java.util.Arrays.copyOf(a, size * 2);
        System.arraycopy(a, i, a, i + 1, size - i); a[i] = s; size++; }
    String remove(int i) { String r = a[i]; System.arraycopy(a, i + 1, a, i, size - i - 1); a[--size] = null; return r; } // O(n)
    String get(int k) { if (k < 0 || k >= size) throw new IndexOutOfBoundsException(); return a[k]; }               // O(1)
    public static void main(String[] x) {
        Q2_ManualArray p = new Q2_ManualArray();
        p.add("Song A"); p.add("Song C"); p.insert(1, "Song B"); p.remove(0);
        for (int i = 0; i < p.size; i++) System.out.println(p.get(i));
    }
}
