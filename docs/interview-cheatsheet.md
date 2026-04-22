# EJB interview cheatsheet (one page)

Review the morning of the interview. Each bullet is the shortest correct
answer; each lesson's README has the long-form version.

## Bean types

| Type | Lifecycle | State | Thread-safety | When to use |
|------|-----------|-------|---------------|-------------|
| `@Stateless` | pooled, each call may use a different instance | none | container serializes per-instance | default for business services / repositories |
| `@Stateful` | one instance per client, passivatable | per-client conversation | serialized per client | multi-step conversations tied to one client (wizard, shopping cart) |
| `@Singleton` | one per JVM | shared | **you** pick: `@Lock(READ/WRITE)` or `BEAN` | caches, rate limiters, scheduled tasks |
| MDB | pooled consumer | none | one message at a time per instance | async JMS processing |

## Transactions

- CMT is the default (`@TransactionAttribute`). BMT only if you truly need multiple short TXs in one call.
- Attributes (memorize): `REQUIRED` (default), `REQUIRES_NEW`, `MANDATORY`, `NEVER`, `NOT_SUPPORTED`, `SUPPORTS`.
- **Rollback rules**: runtime exception => rollback; checked exception => **no** rollback unless `@ApplicationException(rollback=true)` or `ctx.setRollbackOnly()`.
- **Self-invocation does not go through the interceptor chain.** Injecting `SessionContext` and calling `ctx.getBusinessObject(...)` is the fix.
- `REQUIRES_NEW` suspends the caller's TX. Inner TX commits or rolls back independently.

## Concurrency

- `@Singleton` default is `CONTAINER`-managed with `@Lock(WRITE)` on every method. Heavy on contention - annotate read-only methods `@Lock(READ)`.
- `@Asynchronous` methods must return `void`, `Future<V>`, or `CompletionStage<V>`. They run on a container-managed executor.
- `@AccessTimeout` avoids "stuck forever" on lock acquisition.

## JPA in EJB

- `@PersistenceContext` gives a container-managed `EntityManager`. **Never** `new` one.
- `TRANSACTION` PC (default) - stateless services. `EXTENDED` PC - only in Stateful beans.
- `@Version` + optimistic locking is the default answer to "how do you avoid lost updates in a transfer?"

## Timers

- Declarative `@Schedule` for cron-like. Programmatic `TimerService.createCalendarTimer` for dynamic.
- Persistent timers (default) survive restarts and are clustered; non-persistent are node-local.
- Callbacks must be **idempotent** (duplicate fires can happen on failover).

## Messaging (MDB)

- `@MessageDriven` with `activationConfig` for destination + subscription.
- JMS + JDBC in the same method => use an **XA datasource** so both commit atomically.
- Configure `max-delivery-attempts` and a DLQ. Throw runtime to trigger redelivery; `ctx.setRollbackOnly()` works too.

## Security (Elytron)

- Map servlet roles and EJB roles to the same Elytron security domain.
- `@DeclareRoles`, `@RolesAllowed`, `@PermitAll`, `@DenyAll`, `@RunAs`.
- `jakarta.security.enterprise.SecurityContext` for programmatic checks in Jakarta EE 10+.

## Common gotchas (the ones that embarrass candidates)

1. `new MyBean()` - instance is **not** managed; no TX, no injection, no interceptors.
2. Calling `this.otherMethod()` inside the same bean - interceptors/TX skipped.
3. Checked exception without `@ApplicationException(rollback=true)` - TX silently commits.
4. `EntityManager` fields on anything other than a Stateful bean with `EXTENDED` PC.
5. Returning a lazily-initialized collection to the caller after the PC closed - `LazyInitializationException`.
6. `@Singleton` with default `WRITE` lock under load - serialized to 1 req/s throughput.
7. MDB doing non-idempotent writes without checking redelivery count.
8. `@Stateful` without `@Remove` - leaking instances until timeout.
