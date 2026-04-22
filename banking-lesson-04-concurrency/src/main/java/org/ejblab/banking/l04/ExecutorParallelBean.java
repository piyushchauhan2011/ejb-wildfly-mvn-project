package org.ejblab.banking.l04;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedExecutorService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * {@link ManagedExecutorService} is the Jakarta Concurrency analogue of
 * {@code ExecutorService} - container-managed, context-propagating,
 * and thread-pool-tunable. It coexists with {@code @Asynchronous}.
 *
 * <p>When to prefer it:
 * <ul>
 *   <li>When you want to compose using {@link CompletableFuture}
 *       (chain, combine, timeout).</li>
 *   <li>When you want explicit control over which executor (you can
 *       define multiple MES with different pool sizes in WildFly).</li>
 * </ul>
 */
@Stateless
public class ExecutorParallelBean {

    @Resource
    ManagedExecutorService mes;

    public List<String> lookupInParallel(List<String> accountNumbers) throws InterruptedException, ExecutionException {
        var futures = accountNumbers.stream()
                .map(n -> CompletableFuture.supplyAsync(() -> enrich(n), mes))
                .toList();
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.get();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private String enrich(String accountNumber) {
        try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return accountNumber + " | score=" + (300 + Math.abs(accountNumber.hashCode() % 500));
    }
}
