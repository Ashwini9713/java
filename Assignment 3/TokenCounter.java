import java.util.concurrent.ArrayBlockingQueue;

/** Q6 Part B - Producer/consumer with wait()/notifyAll(). TODO 1 and TODO 2 completed. */
public class TokenCounter {
    private final int[] buffer = new int[5];       // the counter holds at most 5 tokens
    private int count = 0, in = 0, out = 0;

    public synchronized void put(int token) throws InterruptedException {
        while (count == buffer.length) wait();     // wait WHILE full (loop guards against spurious/stolen wake-ups)
        buffer[in] = token;
        in = (in + 1) % buffer.length;
        count++;
        notifyAll();                               // wake consumers (and any waiting producer)
    }

    public synchronized int take() throws InterruptedException {
        while (count == 0) wait();                 // wait WHILE empty
        int token = buffer[out];
        out = (out + 1) % buffer.length;
        count--;
        notifyAll();                               // wake the producer
        return token;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounter counter = new TokenCounter();
        int[] collected = new int[2];
        Thread cook = new Thread(() -> {
            try { for (int t = 1; t <= 20; t++) counter.put(t); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try { for (int k = 0; k < 10; k++) collected[id] += counter.take(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        cook.start(); for (Thread t : students) t.start();
        cook.join();  for (Thread t : students) t.join();
        System.out.println("Sum of tokens collected: " + (collected[0] + collected[1]) + " (expected 210)");

        // ---- Alternative: ArrayBlockingQueue(5) - blocking handled by the library ----
        ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(5);
        int[] sum = new int[1];
        Thread p = new Thread(() -> { try { for (int t = 1; t <= 20; t++) q.put(t); } catch (InterruptedException e) { } });
        Thread c = new Thread(() -> { try { for (int k = 0; k < 20; k++) sum[0] += q.take(); } catch (InterruptedException e) { } });
        p.start(); c.start(); p.join(); c.join();
        System.out.println("BlockingQueue version sum: " + sum[0]);
        // To observe the 'while -> if' bug, change both while loops to if and run repeatedly:
        // a woken consumer may find count==0 (another consumer took the token) and read a stale slot,
        // or the producer may overwrite a full buffer -> wrong sum / count outside [0,5].
    }
}
