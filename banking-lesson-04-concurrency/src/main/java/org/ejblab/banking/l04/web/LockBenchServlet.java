package org.ejblab.banking.l04.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l04.ContainerLockedCache;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dirt-simple contention micro-benchmark. Drives {@link ContainerLockedCache}
 * with N threads for a fixed duration and reports ops/sec.
 *
 * <p>Try both:
 * <pre>
 * curl '/bench/lock?threads=16&mode=read'
 * curl '/bench/lock?threads=16&mode=write'
 * </pre>
 * Expected: reads scale ~linearly with cores (READ lock allows concurrency);
 * writes serialize to 1 thread's worth of throughput.
 */
@WebServlet("/bench/lock")
public class LockBenchServlet extends HttpServlet {

    @Inject ContainerLockedCache cache;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int threads = parseInt(req.getParameter("threads"), 8);
        long durationMs = parseInt(req.getParameter("durationMs"), 2_000);
        String mode = req.getParameter("mode");
        boolean write = "write".equalsIgnoreCase(mode);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            long endAt = System.currentTimeMillis() + durationMs;
            var counters = new long[threads];
            var futures = new CompletableFuture<?>[threads];
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    long local = 0;
                    while (System.currentTimeMillis() < endAt) {
                        if (write) cache.record("CHECKING", java.math.BigDecimal.valueOf(local));
                        else cache.rateOf("CHECKING");
                        local++;
                    }
                    counters[idx] = local;
                }, pool);
            }
            CompletableFuture.allOf(futures).join();
            long total = 0; for (long v : counters) total += v;
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("mode=" + (write ? "write" : "read")
                    + " threads=" + threads
                    + " durationMs=" + durationMs
                    + " ops=" + total
                    + " ops/s=" + (total * 1000 / durationMs) + "\n");
        } finally {
            pool.shutdownNow();
        }
    }

    private int parseInt(String s, int d) {
        if (s == null) return d;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return d; }
    }
}
