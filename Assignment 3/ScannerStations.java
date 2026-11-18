import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Q4 - Thread creation, life cycle, join/isAlive, run() vs start(), daemon, ExecutorService. */
public class ScannerStations {
    static final AtomicInteger total = new AtomicInteger();

    static void scan(String station, int scripts) {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(20); } catch (InterruptedException e) { return; }   // TIMED_WAITING
        }
        total.addAndGet(scripts);
        System.out.println(station + " scanned " + scripts + " scripts [running on " + Thread.currentThread().getName() + "]");
    }

    // Way 1: extend Thread
    static class StationThread extends Thread {
        private final int scripts;
        StationThread(String name, int scripts) { super(name); this.scripts = scripts; }
        @Override public void run() { scan(getName(), scripts); }
    }

    // Way 2: implement Runnable (preferred: can still extend another class, reusable with executors)
    static class StationTask implements Runnable {
        private final String name; private final int scripts;
        StationTask(String name, int scripts) { this.name = name; this.scripts = scripts; }
        @Override public void run() { scan(name, scripts); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== 1. extends Thread ==");
        Thread[] t = new Thread[4];
        for (int i = 0; i < 4; i++) t[i] = new StationThread("Station-" + (i + 1), 10 * (i + 1));
        System.out.println("state before start: " + t[0].getState());            // NEW
        for (Thread x : t) x.start();
        System.out.println("alive right after start? " + t[3].isAlive() + ", state " + t[3].getState());
        for (Thread x : t) x.join();                                              // main waits (WAITING)
        System.out.println("alive after join? " + t[3].isAlive() + ", state " + t[3].getState()); // TERMINATED
        System.out.println("TOTAL scanned = " + total.get());

        System.out.println("\n== 2. implements Runnable (priorities set) ==");
        total.set(0);
        Thread[] r = new Thread[4];
        for (int i = 0; i < 4; i++) {
            r[i] = new Thread(new StationTask("Runner-" + (i + 1), 10 * (i + 1)), "R" + (i + 1));
            r[i].setPriority(i == 0 ? Thread.MAX_PRIORITY : Thread.MIN_PRIORITY);  // only a hint to the scheduler
            r[i].start();
        }
        for (Thread x : r) x.join();
        System.out.println("TOTAL scanned = " + total.get());

        System.out.println("\n== 3. run() vs start() ==");
        Thread demo = new Thread(new StationTask("Direct-run", 1), "demo-thread");
        demo.run();                      // ordinary method call -> prints 'main'
        demo.setName("demo-thread-2");
        Thread demo2 = new Thread(new StationTask("Started", 1), "demo-thread-2");
        demo2.start(); demo2.join();     // prints 'demo-thread-2'

        System.out.println("\n== 4. ExecutorService, fixed pool of 2 threads, 4 stations ==");
        total.set(0);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 4; i++) pool.submit(new StationTask("Pool-job-" + (i + 1), 10 * (i + 1)));
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("TOTAL scanned = " + total.get());

        System.out.println("\n== 5. daemon station: killed when main ends (no 'scanned' line appears) ==");
        Thread daemon = new StationThread("Daemon-Station", 500);
        daemon.setDaemon(true);
        daemon.start();
        System.out.println("main ends now");
    }
}
