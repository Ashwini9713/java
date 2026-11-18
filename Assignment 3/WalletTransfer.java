import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/** Q6 Part A - Wallet transfers: deadlock version, lock-ordering fix, and tryLock-with-timeout alternative. */
public class WalletTransfer {
    static class Account {
        final int id; int balance; final ReentrantLock lock = new ReentrantLock();
        Account(int id, int balance) { this.id = id; this.balance = balance; }
    }
    static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }

    // Deadlock-prone: lock order depends on the direction of the transfer
    static void transferDeadlock(Account from, Account to, int amt) {
        synchronized (from) {
            pause(100);                                   // lets the other thread grab its first lock
            synchronized (to) { from.balance -= amt; to.balance += amt; }
        }
    }

    // Fix: always lock the account with the smaller id first (breaks circular wait)
    static void transferSafe(Account from, Account to, int amt) {
        Account first  = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            pause(100);
            synchronized (second) { from.balance -= amt; to.balance += amt; }
        }
    }

    // Alternative: tryLock with timeout - give up and report instead of blocking forever
    static boolean transferTryLock(Account from, Account to, int amt) throws InterruptedException {
        if (from.lock.tryLock(200, TimeUnit.MILLISECONDS)) {
            try {
                pause(100);
                if (to.lock.tryLock(200, TimeUnit.MILLISECONDS)) {
                    try { from.balance -= amt; to.balance += amt; return true; }
                    finally { to.lock.unlock(); }
                }
            } finally { from.lock.unlock(); }
        }
        return false;                                     // caller may retry
    }

    public static void main(String[] args) throws Exception {
        // ---- deadlock ----
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> transferDeadlock(a, b, 100), "T1 (A->B)");
        Thread t2 = new Thread(() -> transferDeadlock(b, a, 50),  "T2 (B->A)");
        t1.setDaemon(true); t2.setDaemon(true);           // daemon so the JVM can still exit
        t1.start(); t2.start();
        t1.join(1500); t2.join(1500);
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] dead = mx.findDeadlockedThreads();
        System.out.println("Deadlock version  -> T1 alive: " + t1.isAlive() + ", T2 alive: " + t2.isAlive()
                + ", deadlocked threads detected: " + (dead == null ? 0 : dead.length));

        // ---- fixed by lock ordering ----
        Account c = new Account(1, 1000), d = new Account(2, 1000);
        Thread t3 = new Thread(() -> transferSafe(c, d, 100));
        Thread t4 = new Thread(() -> transferSafe(d, c, 50));
        t3.start(); t4.start(); t3.join(); t4.join();
        System.out.println("Lock-ordered      -> balances: " + c.balance + " / " + d.balance + " (total " + (c.balance + d.balance) + ")");

        // ---- tryLock alternative ----
        Account e = new Account(1, 1000), f = new Account(2, 1000);
        boolean[] ok = new boolean[2];
        Thread t5 = new Thread(() -> { try { ok[0] = transferTryLock(e, f, 100); } catch (InterruptedException x) { } });
        Thread t6 = new Thread(() -> { try { ok[1] = transferTryLock(f, e, 50);  } catch (InterruptedException x) { } });
        t5.start(); t6.start(); t5.join(); t6.join();
        System.out.println("tryLock + timeout -> transfer1 ok: " + ok[0] + ", transfer2 ok: " + ok[1] + " (no hang; failed one can retry)");
    }
}
