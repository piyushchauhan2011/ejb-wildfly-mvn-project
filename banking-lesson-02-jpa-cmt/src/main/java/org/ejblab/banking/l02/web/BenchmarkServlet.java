package org.ejblab.banking.l02.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l02.LoadDataBean;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Hits {@link LoadDataBean#insertBatch(int)} and reports wall-clock duration.
 *
 * <p>Usage:
 * <pre>{@code
 * curl 'http://localhost:8080/banking-lesson-02-jpa-cmt/benchmark/batch?n=1000'
 * curl 'http://localhost:8080/banking-lesson-02-jpa-cmt/benchmark/one-per-tx?n=1000'
 * }</pre>
 */
@WebServlet({"/benchmark/batch", "/benchmark/one-per-tx"})
public class BenchmarkServlet extends HttpServlet {

    @Inject
    LoadDataBean bench;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int n = parseIntOrDefault(req.getParameter("n"), 1_000);
        String path = req.getServletPath();
        long nanos;
        if (path.endsWith("/one-per-tx")) {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                bench.insertOnePerTx();
            }
            nanos = System.nanoTime() - t0;
        } else {
            nanos = bench.insertBatch(n);
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("n=" + n
                + " duration=" + millis + " ms"
                + " rows/s=" + (n * 1000L / Math.max(1, millis))
                + "\n");
    }

    private static int parseIntOrDefault(String s, int d) {
        if (s == null) return d;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return d; }
    }
}
