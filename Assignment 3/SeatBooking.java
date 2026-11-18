import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/** Q5 - Race condition and synchronization. TODO 1 = the synchronized book() method. */
public class SeatBooking {
    private final boolean[] booked;
    private int bookingsMade = 0;
    private final Object lock = new Object();
    SeatBooking(int seats) { booked = new boolean[seats]; }

    // Unsafe: check-then-act is not atomic (yield() widens the window so the race shows reliably)
    void bookUnsafe(int seat) {
        if (!booked[seat]) {
            Thread.yield();
            booked[seat] = true;
            bookingsMade++;
        }
    }

    // TODO 1 (done): synchronized METHOD - lock on 'this'
    synchronized void book(int seat) {
        if (!booked[seat]) {
            booked[seat] = true;
            bookingsMade++;
        }
    }

    // synchronized BLOCK - lock only the critical section
    void bookBlock(int seat) {
        synchronized (lock) {
            if (!booked[seat]) { booked[seat] = true; bookingsMade++; }
        }
    }

    // Alternative: AtomicBoolean[] + compareAndSet (lock-free, per-seat)
    static int runAtomic() throws InterruptedException {
        AtomicBoolean[] seats = new AtomicBoolean[10];
        for (int i = 0; i < 10; i++) seats[i] = new AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicInteger sold = new java.util.concurrent.atomic.AtomicInteger();
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;
            users[i] = new Thread(() -> { if (seats[seat].compareAndSet(false, true)) sold.incrementAndGet(); });
            users[i].start();
        }
        for (Thread t : users) t.join();
        return sold.get();
    }

    static int run(BiConsumer<SeatBooking, Integer> strategy) throws InterruptedException {
        SeatBooking screen = new SeatBooking(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;                       // 10 users compete for every seat
            users[i] = new Thread(() -> strategy.accept(screen, seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        return screen.bookingsMade;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Unsynchronized, 10 runs (expected 10 but may be more):");
        for (int r = 1; r <= 10; r++) System.out.println("  run " + r + ": Seats sold: " + run(SeatBooking::bookUnsafe));

        System.out.println("Synchronized method, 10 runs:");
        for (int r = 1; r <= 10; r++) System.out.println("  run " + r + ": Seats sold: " + run(SeatBooking::book));

        System.out.println("Synchronized block: Seats sold: " + run(SeatBooking::bookBlock));
        System.out.println("AtomicBoolean CAS : Seats sold: " + runAtomic());
    }
}
