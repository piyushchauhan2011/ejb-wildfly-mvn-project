package org.ejblab.banking.l04;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link Asynchronous} methods run on a container-managed executor.
 *
 * <p>Rules (memorize):
 * <ul>
 *   <li>Return type must be {@code void}, {@link Future}, or {@link java.util.concurrent.CompletionStage}.</li>
 *   <li>Return {@code void} = fire-and-forget; exceptions are logged, not propagated.</li>
 *   <li>Return a {@code Future} = caller can await the result with
 *       {@link Future#get()}; the container wraps exceptions in
 *       {@link java.util.concurrent.ExecutionException}.</li>
 *   <li>The caller's TX context is NOT automatically propagated - each
 *       async method gets its own TX (same {@code @TransactionAttribute} rules).</li>
 * </ul>
 */
@Stateless
public class AsyncNotificationBean {

    private static final Logger log = Logger.getLogger(AsyncNotificationBean.class.getName());

    /** Fire-and-forget email / webhook. */
    @Asynchronous
    public void sendTransferReceipt(String email, String text) {
        simulateSlowIO(150);
        log.info("email to " + email + ": " + text);
    }

    /** Returns a Future so the caller can wait on completion or pick up the result. */
    @Asynchronous
    public Future<String> lookupCreditScore(String accountNumber) {
        simulateSlowIO(100);
        int score = 300 + Math.abs(accountNumber.hashCode() % 500);
        return new AsyncResult<>(accountNumber + " -> " + score);
    }

    private void simulateSlowIO(long millis) {
        try { TimeUnit.MILLISECONDS.sleep(millis); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
