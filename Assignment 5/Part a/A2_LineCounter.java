import java.io.*;
import java.nio.file.*;
import java.util.List;

/**
 * TASK A2 - Don't read what you don't need.
 *
 * A2.1 Prediction: FileInputStream.read() makes ONE read call per byte => 50 MB =
 *      52,428,800 calls (+1 for EOF). Each call crosses into the OS (JNI + system call),
 *      so cost is dominated by call overhead, not by disk transfer.
 * A2.2 Redesign: BufferedInputStream / byte[8192] -> 52,428,800 / 8,192 = 6,400 calls
 *      (8192x fewer); with byte[65536] only 800 calls.
 * A2.3 No benefit: when each read already asks for a large block (e.g. read(byte[1 MB])),
 *      or when access is random (seek + read a few bytes), or the data is read just once
 *      in a single bulk transfer - the buffer only adds a copy.
 * Alternative: Files.readAllLines -> O(n) time, O(file size) memory (>50 MB of chars, ~100+ MB
 *      on heap as Strings) vs buffered O(n) time, O(buffer) memory (8 KB). Use readAllLines
 *      for small files you need as a List; use buffered for large/unknown-size files.
 */
public class A2_LineCounter {
    static long readCalls;

    static long countUnbuffered(Path p) throws IOException {
        long lines = 0; readCalls = 0;
        try (FileInputStream in = new FileInputStream(p.toFile())) {
            int b;
            while ((b = in.read()) != -1) { readCalls++; if (b == '\n') lines++; }
            readCalls++;                                   // final call returning -1
        }
        return lines;
    }

    static long countBuffered(Path p, int bufSize) throws IOException {
        long lines = 0; readCalls = 0;
        byte[] buf = new byte[bufSize];
        try (FileInputStream in = new FileInputStream(p.toFile())) {
            int n;
            while ((n = in.read(buf)) != -1) {             // one OS call per bufSize bytes
                readCalls++;
                for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
            }
            readCalls++;
        }
        return lines;
    }

    static long countBufferedReader(Path p) throws IOException {
        long lines = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(p.toFile()))) {
            while (r.readLine() != null) lines++;
        }
        return lines;
    }

    static long countReadAllLines(Path p) throws IOException {
        List<String> all = Files.readAllLines(p);          // whole file in memory
        return all.size();
    }

    /** Creates a file of about 'mb' megabytes of attendance-log lines. */
    static Path makeLog(int mb) throws IOException {
        Path f = Files.createTempFile("attendance", ".log");
        byte[] line = "192311001,2026-11-22,PRESENT\n".getBytes();
        long target = mb * 1024L * 1024L;
        try (BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(f.toFile()), 1 << 16)) {
            for (long w = 0; w < target; w += line.length) o.write(line);
        }
        return f;
    }

    public static void main(String[] args) throws IOException {
        int mb = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        Path f = makeLog(mb);
        long size = Files.size(f);
        System.out.printf("File size = %,d bytes. Predicted unbuffered read calls = %,d; buffered(8192) = %,d%n",
                size, size + 1, (size + 8191) / 8192 + 1);
        long t = System.nanoTime(); long l1 = countUnbuffered(f); long c1 = readCalls;
        double ms1 = (System.nanoTime() - t) / 1e6;
        t = System.nanoTime(); long l2 = countBuffered(f, 8192); long c2 = readCalls;
        double ms2 = (System.nanoTime() - t) / 1e6;
        t = System.nanoTime(); long l3 = countBufferedReader(f);
        double ms3 = (System.nanoTime() - t) / 1e6;
        t = System.nanoTime(); long l4 = countReadAllLines(f);
        double ms4 = (System.nanoTime() - t) / 1e6;
        System.out.printf("unbuffered      : %,d lines, %,d calls, %.1f ms%n", l1, c1, ms1);
        System.out.printf("byte[8192]      : %,d lines, %,d calls, %.1f ms%n", l2, c2, ms2);
        System.out.printf("BufferedReader  : %,d lines, %.1f ms%n", l3, ms3);
        System.out.printf("readAllLines    : %,d lines, %.1f ms (heap = O(file size))%n", l4, ms4);
        Files.delete(f);
    }
}
