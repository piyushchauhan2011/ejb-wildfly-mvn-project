package org.ejblab.banking.l04;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.AccessTimeout;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Container-managed concurrency on a {@code @Singleton}.
 *
 * <p>Defaults remind:
 * <ul>
 *   <li>{@code @ConcurrencyManagement(CONTAINER)} is the default.</li>
 *   <li>Every method is implicitly {@code @Lock(WRITE)}; override on reads.</li>
 *   <li>{@code @AccessTimeout} prevents forever-blocking on lock acquisition.</li>
 * </ul>
 *
 * <p>We use a {@link ConcurrentHashMap} under the hood so read-only methods
 * could be safely {@link LockType#READ}; writes remain {@link LockType#WRITE}
 * because of the "compute-update-store" pattern in {@link #record(String, BigDecimal)}.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@AccessTimeout(value = 5, unit = TimeUnit.SECONDS)
public class ContainerLockedCache {

    private final Map<String, BigDecimal> interestRates = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        interestRates.put("CHECKING", new BigDecimal("0.005"));
        interestRates.put("SAVINGS",  new BigDecimal("0.0225"));
        interestRates.put("LOAN",     new BigDecimal("0.0625"));
    }

    @Lock(LockType.READ)
    public Optional<BigDecimal> rateOf(String type) {
        return Optional.ofNullable(interestRates.get(type));
    }

    @Lock(LockType.READ)
    public int size() { return interestRates.size(); }

    @Lock(LockType.WRITE)
    public void record(String type, BigDecimal rate) {
        interestRates.put(type, rate);
    }
}
