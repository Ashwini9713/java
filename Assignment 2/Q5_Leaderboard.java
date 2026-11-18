import java.util.*;

/** Q5: Generic leaderboard driven by a consistent Comparator. */
public class Q5_Leaderboard {
    public static void main(String[] args) {
        // TODO 2
        Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                .thenComparing(t -> t.name);
        Leaderboard<Team> board = new Leaderboard<>(byRank);
        board.add(new Team("Panthers", 9, 4)); board.add(new Team("Eagles", 9, 6));
        board.add(new Team("Cobras", 6, 1));   board.add(new Team("Bisons", 9, 4));
        System.out.println(board.ranking());   // [Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]

        // Data set with ties at every level
        List<Team> ties = List.of(new Team("D", 5, 2), new Team("B", 5, 2), new Team("C", 5, 3), new Team("A", 7, 0), new Team("E", 7, 0), new Team("F", 3, -1));
        Leaderboard<Team> b2 = new Leaderboard<>(byRank); b2.addAll(ties);
        System.out.println("ties data   : " + b2.ranking());

        // Faulty comparators
        Comparator<Team> subtraction = (x, y) -> y.points - x.points;                       // overflow risk, no tie-breakers
        Comparator<Team> noTieBreak = Comparator.comparingInt((Team t) -> t.points).reversed().thenComparing(t -> t.name); // ignores goalDiff
        System.out.println("ignore GD   : " + sorted(ties, noTieBreak) + "  <- C(5,3) wrongly below B(5,2)");
        List<Team> extreme = List.of(new Team("Max", Integer.MAX_VALUE, 0), new Team("Neg", -5, 0), new Team("Zero", 0, 0));
        Comparator<Team> overflow = (x, y) -> y.points - x.points;
        try { System.out.println("subtraction : " + sorted(extreme, overflow) + "  <- Neg(-5) vs Max: -5-MAX is fine but MAX-(-5) overflows -> wrong sign"); }
        catch (IllegalArgumentException e) { System.out.println("subtraction : TimSort detected contract violation: " + e.getMessage()); }
        System.out.println("correct     : " + sorted(extreme, byRank));

        // Sorting 3 times with stable sorts: same answer, 3 sorts instead of 1
        List<Team> l = new ArrayList<>(ties);
        l.sort(Comparator.comparing((Team t) -> t.name));
        l.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
        l.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
        System.out.println("3 stable    : " + l + "  equal to chain? " + l.toString().equals(b2.ranking().toString()));
    }
    static List<Team> sorted(List<Team> in, Comparator<Team> c) { List<Team> l = new ArrayList<>(in); l.sort(c); return l; }
}

class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) { this.name = name; this.points = points; this.goalDiff = goalDiff; }
    @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
}
class Leaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;
    Leaderboard(Comparator<? super T> order) { this.order = order; }
    void add(T item) { items.add(item); }
    void addAll(List<? extends T> more) { items.addAll(more); }          // wildcard: producer (PECS: extends)
    List<T> ranking() { List<T> copy = new ArrayList<>(items); copy.sort(order); return copy; }  // TODO 1
    static <E extends Comparable<? super E>> List<E> rankNatural(Collection<? extends E> c) {     // bounded type
        List<E> l = new ArrayList<>(c); Collections.sort(l); return l; }
}
