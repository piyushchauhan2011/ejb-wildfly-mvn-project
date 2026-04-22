package org.ejblab.banking.l04;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class Lesson04IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "lesson04-it.war")
                .addClasses(ContainerLockedCache.class, BeanManagedCache.class,
                        RateLimiterSingleton.class, AsyncNotificationBean.class,
                        ExecutorParallelBean.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject ContainerLockedCache cache;
    @Inject BeanManagedCache bean;
    @Inject RateLimiterSingleton limiter;
    @Inject AsyncNotificationBean async;

    @Test
    void read_lock_allows_concurrent_reads_smoke() {
        assertEquals(3, cache.size());
        assertTrue(cache.rateOf("CHECKING").isPresent());
    }

    @Test
    void bean_managed_increment_is_atomic() {
        long before = bean.read("transfers").orElse(0L);
        bean.increment("transfers");
        bean.increment("transfers");
        assertEquals(before + 2, bean.read("transfers").orElseThrow());
    }

    @Test
    void rate_limiter_denies_after_capacity() {
        String key = "unit-test-" + System.nanoTime();
        int granted = 0;
        for (int i = 0; i < 100; i++) {
            if (limiter.tryAcquire(key)) granted++;
        }
        // Capacity is 5, refill is slow relative to a tight loop, so we expect
        // something close to the initial burst capacity.
        assertTrue(granted >= 5 && granted <= 10, "granted was " + granted);
    }

    @Test
    void async_future_completes() throws InterruptedException, ExecutionException {
        Future<String> f = async.lookupCreditScore("ACC-001");
        assertTrue(f.get().contains("ACC-001"));
    }
}
