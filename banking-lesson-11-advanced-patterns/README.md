# Lesson 11 — Advanced Patterns

Four deep cuts you will absolutely get asked about: **Stateful lifecycle**, **extended persistence context**, **optimistic locking + retry interceptor**, and **Bean Validation on EJB methods**.

## 1. Stateful lifecycle & passivation — `BasketBean`

```java
@Stateful
@SessionScoped             // lets CDI manage scoping per HttpSession
@StatefulTimeout(value = 15, unit = TimeUnit.MINUTES)
public class BasketBean implements Serializable {

    private final List<String> items = new ArrayList<>();

    public void add(String sku)   { items.add(sku); }

    @PrePassivate  void passivate() { /* flush non-serializable */ }
    @PostActivate  void activate()  { /* re-acquire resources */ }

    @Remove
    public void checkout() { /* bean destroyed after this returns */ }
}
```

Rules:
- **`Serializable` or you don't passivate.** Any non-serializable field must be nulled in `@PrePassivate` and restored in `@PostActivate`.
- **`@Remove` is the client's way to END the conversation.** Without it you wait for `@StatefulTimeout` → leaked memory, leaked DB connections (if you held them).
- **`@SessionScoped` + `@Stateful`** is the right way to inject a per-HttpSession stateful bean via CDI. Without a CDI scope, the default is `Dependent` — a fresh SFSB per injection point, which is almost never what you want.
- **Passivation stores serialize state to disk** (`data/passivation-store`) when the SFSB cache is full. Tune via `subsystem=ejb3/passivation-store=infinispan`.

## 2. EXTENDED persistence context — `AccountEditor`

```java
@Stateful
public class AccountEditor {
    @PersistenceContext(unitName = "bankingPU", type = EXTENDED)
    EntityManager em;

    Account current;

    public Account load(Long id)   { return current = em.find(Account.class, id); }
    public void credit(BigDecimal d) { current.setBalance(current.getBalance().add(d)); }
    @Remove public void save()     { em.flush(); }
}
```

Why this exists: you want entities to **stay managed across multiple method calls** (a multi-step edit wizard, long conversation, etc.). With a `TRANSACTION`-scoped context, the entity goes detached at the end of every call — you'd have to `merge()` every time.

Rules (break them at your peril):
- **Only Stateful** can hold EXTENDED.
- **Between calls there's NO transaction.** The persistence context still exists but changes aren't flushed. The first method that runs under a tx (default `REQUIRED`) flushes everything accumulated.
- **Don't share with transactional EntityManagers on the same thread.** "More than one EntityManager in persistence context" means a transactional bean you called also has a `@PersistenceContext` pointing at the same PU. The container detects the conflict.

## 3. Optimistic locking + `@Retryable` interceptor

`Account.version` is `@Version` already (Lesson 2). Two concurrent updates produce one `OptimisticLockException`. We want the business method to transparently retry.

```java
@Retryable(maxAttempts = 5, backoffMillis = 10)
public BigDecimal adjust(String account, @Valid TransferCommand cmd) { ... }
```

`RetryInterceptor` at `@Priority(Interceptor.Priority.APPLICATION)` wraps the call, catches `OptimisticLockException` (and its Hibernate wrappers), and re-invokes — which, because the method is `REQUIRED`, opens a **new transaction per attempt**. That's critical: you can't retry inside the same failed transaction.

### Why is the interceptor outside the transaction?

| Priority            | Interceptor                           |
|---------------------|----------------------------------------|
| `PLATFORM_BEFORE`   | System interceptors (auth, etc.)       |
| `APPLICATION`       | **Your `RetryInterceptor`** ← here     |
| `LIBRARY_BEFORE`    | Library interceptors                   |
| `(tx-interceptor)`  | Container's tx interceptor             |
| Target method       |                                        |

Because retry sits at `APPLICATION` priority and the tx interceptor is below it, each `ctx.proceed()` opens a fresh tx — exactly what we need. If you put the retry logic BELOW the tx interceptor, you'd be retrying inside a rolled-back transaction, which does nothing.

### Activate the interceptor

In `beans.xml`:

```xml
<interceptors>
    <class>org.ejblab.banking.l11.RetryInterceptor</class>
</interceptors>
```

Even though we use `@Priority`, explicitly listing in `beans.xml` keeps ordering unambiguous across the project.

## 4. Bean Validation on EJB methods — `AdjustmentService`

```java
public BigDecimal adjust(@NotNull String acct, @Valid @NotNull TransferCommand cmd) { ... }
```

The container's EJB interceptor chain runs Bean Validation on method parameters. Violations throw `ConstraintViolationException`, which rolls back the surrounding tx. No glue code, no filter, no Spring `@Validated`.

The DTO:

```java
public record TransferCommand(
    @NotBlank @Size(max = 34) String fromAccount,
    @NotBlank @Size(max = 34) String toAccount,
    @NotNull  @DecimalMin("0.01") BigDecimal amount) implements Serializable { }
```

Adding the `bean-validation` Galleon layer pulls Hibernate Validator in; no other dependency needed.

## Pitfalls

1. **`@Version` does NOT auto-increment** for unmanaged entities. If you `merge()` a detached entity with the old version, the merge succeeds but any stale writes get rejected at flush. Always reload inside the tx.
2. **Retry on the wrong exception hierarchy.** `OptimisticLockException` arrives wrapped in `EJBTransactionRolledbackException` from an EJB client. The interceptor walks `getCause()` to detect it.
3. **EXTENDED persistence contexts kept too long** pin every loaded entity in memory. Don't treat them as a cache — they aren't one. Always `@Remove` or call `em.clear()`.
4. **Bean Validation constraints on a method parameter** are silently ignored if the EJB has no interceptor bindings wired. The default EE integration auto-wires a `MethodValidationInterceptor`, but if you use a custom `<interceptors>` list in `beans.xml` and forget it, validation stops. Easiest: don't list the default interceptors.
5. **`@Remove` on a method that throws** still destroys the SFSB. If you need to keep state on error, check `retainIfException=true`.
6. **Passivation is disabled by default in WildFly for Hibernate Session**. If you store non-serializable objects in an SFSB, config either suppresses passivation entirely (leak risk) or corrupts the session. Only store serializable state.

## Interview Q&A

**Q: `@Stateful` vs CDI `@SessionScoped` bean — when would you pick each?**
A: SFSB gives you a container-managed conversation with passivation, `@StatefulTimeout`, and EXTENDED persistence context support. CDI `@SessionScoped` gives you one-bean-per-HTTP-session with simpler semantics and NO passivation (unless Weld-Infinispan integration is enabled). Rule of thumb: if you need JTA-aware, container-passivated state that survives GC pressure, use `@Stateful`. For plain web-session state, `@SessionScoped` is lighter.

**Q: When is EXTENDED persistence context dangerous?**
A: Under load. It keeps the persistence-context state alive for the session's duration. An SFSB timeout of 30m × 10k sessions × 50 entities = a lot of RAM. Keep the session short or evict aggressively.

**Q: Why use optimistic locking instead of pessimistic?**
A: Optimistic wins when writes rarely conflict. It scales better — no DB row locks, no deadlocks, commit-time check only. Pessimistic wins when conflicts are frequent (hot-row scenarios like an inventory counter): a row-level lock is cheaper than retry loops. Banking balances are typically medium-contention — both work; benchmark on real traffic.

**Q: How do you retry without retrying forever?**
A: Cap attempts, add backoff, categorize errors (transient vs permanent). In this lesson's `RetryInterceptor` we cap at 5 attempts and only retry optimistic-lock failures, not business exceptions.

**Q: What's wrong with `@Valid` on a class-level?**
A: Class-level `@Valid` validates the **return value** only. To validate parameters you put `@Valid` on each parameter (or use `@ValidateOnExecution`).

## Run

```bash
./mvnw -pl banking-lesson-11-advanced-patterns wildfly:provision wildfly:deploy

curl -X POST -d 'from=ADV-001&to=ADV-002&amount=10.00' \
  http://localhost:8080/banking-lesson-11-advanced-patterns/adjust
# -> new balance=1010.00

curl "http://localhost:8080/banking-lesson-11-advanced-patterns/basket/add?sku=X"
curl "http://localhost:8080/banking-lesson-11-advanced-patterns/basket/add?sku=Y"
curl "http://localhost:8080/banking-lesson-11-advanced-patterns/basket/checkout"

curl -X POST -d 'from=ADV-001&to=ADV-002&amount=-1.00' \
  http://localhost:8080/banking-lesson-11-advanced-patterns/adjust
# -> 400 validation failed: ...
```
