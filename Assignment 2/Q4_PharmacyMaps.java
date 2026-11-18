import java.util.*;

/** Q4: HashMap vs TreeMap vs LinkedHashMap, plus sorted array + binarySearch. */
public class Q4_PharmacyMaps {
    static volatile long sink;
    record Stock(String code, int qty) {}
    static double ms(Runnable r) { long t = System.nanoTime(); r.run(); return (System.nanoTime() - t) / 1e6; }

    public static void main(String[] args) {
        System.out.printf("%-14s %8s %10s %12s %12s %14s%n", "Map", "n", "put all", "100k get", "iterate", "report order");
        for (int n : new int[]{1_000, 20_000, 200_000}) {
            String[] codes = new String[n];
            Random r = new Random(7);
            for (int i = 0; i < n; i++) codes[i] = String.format("MED%07d", r.nextInt(10_000_000) * 1L % 10_000_000 + i * 0); // random codes
            Set<String> uniq = new LinkedHashSet<>(Arrays.asList(codes)); String[] keys = uniq.toArray(new String[0]);
            for (String kind : new String[]{"HashMap", "TreeMap", "LinkedHashMap"}) {
                double put = 0, get = 0, it = 0; boolean sorted = false, insertion = false;
                for (int rep = 0; rep < 3; rep++) {
                    Map<String, Stock> m = kind.equals("HashMap") ? new HashMap<>() : kind.equals("TreeMap") ? new TreeMap<>() : new LinkedHashMap<>();
                    double p = ms(() -> { for (String c : keys) m.put(c, new Stock(c, 10)); });
                    double g = ms(() -> { long s = 0; for (int i = 0; i < 100_000; i++) s += m.get(keys[(i * 31) % keys.length]).qty(); sink = s; });
                    double itr = ms(() -> { long s = 0; for (Stock st : m.values()) s += st.qty(); sink = s; });
                    if (rep > 0) { put += p / 2; get += g / 2; it += itr / 2; }
                    List<String> order = new ArrayList<>(m.keySet());
                    List<String> srt = new ArrayList<>(order); Collections.sort(srt);
                    sorted = order.equals(srt); insertion = order.equals(Arrays.asList(keys));
                }
                System.out.printf("%-14s %8d %10.1f %12.1f %12.1f   sorted=%b insertion=%b%n", kind, keys.length, put, get, it, sorted, insertion);
            }
            // extra work if the wrong map is used: HashMap -> code order report = sort keys
            Map<String, Stock> hm = new HashMap<>(); for (String c : keys) hm.put(c, new Stock(c, 1));
            double sortCost = ms(() -> { List<String> k = new ArrayList<>(hm.keySet()); Collections.sort(k); sink = k.size(); });
            Map<String, Stock> tm = new TreeMap<>(); for (String c : keys) tm.put(c, new Stock(c, 1));
            System.out.printf("   extra work: HashMap code-order report needs sort = %.1f ms; TreeMap cannot give insertion order at all (needs separate list)%n", sortCost);

            // sorted array + binarySearch
            Stock[] arr = Arrays.stream(keys).sorted().map(c -> new Stock(c, 1)).toArray(Stock[]::new);
            String[] sk = Arrays.stream(arr).map(Stock::code).toArray(String[]::new);
            double bs = ms(() -> { long s = 0; for (int i = 0; i < 100_000; i++) s += Arrays.binarySearch(sk, keys[(i * 31) % keys.length]); sink = s; });
            double ins = ms(() -> { Stock[] c = Arrays.copyOf(arr, arr.length + 1); System.arraycopy(c, 0, c, 1, arr.length); sink = c.length; }); // one worst-case insertion shift
            System.out.printf("   sorted array: 100k binarySearch = %.1f ms; one worst-case insertion (shift) = %.3f ms%n%n", bs, ins);
        }
    }
}
