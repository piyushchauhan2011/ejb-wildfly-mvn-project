package org.ejblab.banking.l01;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory "catalog" of accounts so Lesson 1 has no DB dependency.
 *
 * <p>Three things Lesson 1 showcases about {@code @Singleton}:
 * <ol>
 *   <li>{@link Startup} = eagerly instantiated at deploy time (useful for
 *       warm-up, cache priming, or a JNDI resource check).</li>
 *   <li>{@link ConcurrencyManagement}({@link ConcurrencyManagementType#CONTAINER})
 *       is the default. Methods default to {@link LockType#WRITE} unless
 *       overridden - every call is serialized, which kills throughput.</li>
 *   <li>Reads annotated {@link LockType#READ} permit concurrent callers,
 *       writes still exclude readers.</li>
 * </ol>
 *
 * <p>Lesson 4 revisits this pattern with {@code BEAN}-managed concurrency
 * (DIY with {@code ReadWriteLock}) and a rate-limiter example.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class AccountCatalogSingleton {

    private final Map<String, String> accountNumberToOwner = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        accountNumberToOwner.put("GB29NWBK60161331926819", "Ada Lovelace");
        accountNumberToOwner.put("DE89370400440532013000", "Alan Turing");
        accountNumberToOwner.put("US12345678901234567890", "Grace Hopper");
    }

    @Lock(LockType.READ)
    public Optional<String> ownerOf(String accountNumber) {
        return Optional.ofNullable(accountNumberToOwner.get(accountNumber));
    }

    @Lock(LockType.READ)
    public int size() { return accountNumberToOwner.size(); }

    @Lock(LockType.WRITE)
    public void register(String accountNumber, String owner) {
        accountNumberToOwner.put(accountNumber, owner);
    }
}
