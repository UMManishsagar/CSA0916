package support;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/** Small educational harness, not a replacement for JMH. Setup is outside timing. */
public final class Bench implements AutoCloseable {
    public static volatile long sink;
    private final PrintWriter raw;
    private final String task;
    public Bench(String task) throws IOException {
        this.task = task;
        Path dir = Paths.get(task, "results");
        Files.createDirectories(dir);
        raw = new PrintWriter(Files.newBufferedWriter(dir.resolve("raw.csv")));
        raw.println("task,case,size,operations,repeat,nanos,heap_before,heap_after,checksum");
        System.out.println(task + " | Java " + System.getProperty("java.version")
            + " | " + System.getProperty("os.name") + " | CPUs="
            + Runtime.getRuntime().availableProcessors());
    }
    private static long heap() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }
    public double measure(String label, int size, int operations,
                          Supplier<LongSupplier> setup) {
        for (int w = 0; w < 3; w++) sink = setup.get().getAsLong();
        long total = 0;
        for (int r = 1; r <= 7; r++) {
            LongSupplier action = setup.get();
            long before = heap(), start = System.nanoTime();
            long result = action.getAsLong();
            long elapsed = System.nanoTime() - start, after = heap();
            sink = result; total += elapsed;
            raw.printf(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%d,%d,%d%n",
                task, label, size, operations, r, elapsed, before, after, result);
        }
        raw.flush();
        double ns = total / 7.0 / operations;
        System.out.printf(Locale.ROOT, "%s n=%d ops=%d mean=%.2f ns/op%n",
            label, size, operations, ns);
        return ns;
    }
    public static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public void close() { raw.close(); }
}
