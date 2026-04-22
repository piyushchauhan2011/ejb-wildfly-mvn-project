package org.ejblab.banking.l04.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l04.AsyncNotificationBean;
import org.ejblab.banking.l04.RateLimiterSingleton;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fakes a transfer endpoint to demo the rate limiter and async patterns.
 * Real transfers live in Lessons 3, 7, 10.
 */
@WebServlet("/transfer")
public class RateLimitedTransferServlet extends HttpServlet {

    @Inject RateLimiterSingleton limiter;
    @Inject AsyncNotificationBean async;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String key = req.getParameter("from");
        if (key == null) key = req.getRemoteAddr();

        if (!limiter.tryAcquire(key)) {
            resp.setStatus(429); // Too Many Requests — no constant in jakarta.servlet
            resp.getWriter().write("rate limit: try again soon\n");
            return;
        }

        // Fire-and-forget email
        async.sendTransferReceipt(req.getParameter("email"),
                "transfer " + req.getParameter("amount") + " OK");

        // Wait briefly for a credit-score lookup
        Future<String> score = async.lookupCreditScore(req.getParameter("from"));
        String result;
        try {
            result = score.get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            result = "(lookup not available yet)";
        }

        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("transfer OK; " + result + "\n");
    }
}
