# Lesson 6 - Timers & Scheduling

> **Goal:** know when to use `@Schedule` vs programmatic timers, what
> "persistent" actually means in a cluster, and how to write a timer
> callback that survives duplicate fires.

## The API in one table

| You want | Use |
| --- | --- |
| Cron-like, known at compile time | `@Schedule(hour="0", minute="15")` |
| Fire once after N millis | `TimerService.createSingleActionTimer(...)` |
| Cron-like, chosen at runtime | `TimerService.createCalendarTimer(schedule, cfg)` |
| Interval timer (every N ms) | `TimerService.createIntervalTimer(...)` |
| Cancel or list existing | `Timer.cancel()`, `TimerService.getTimers()` |

## Persistent vs non-persistent

| Flag | Survives restart? | Visible across nodes? | Where to use |
| --- | --- | --- | --- |
| `persistent=true` (default) | yes | yes - cluster-wide, one fire per cluster | business logic you don't want to miss (nightly reports, reconciliations) |
| `persistent=false` | no - gone on restart | no - local | UI countdowns, in-memory caches, dev-only |

WildFly stores persistent timers in the `ejb3` subsystem's timer store
(file-based by default; can be configured to a JDBC store for true
cluster safety - see the lesson section below).

## What you'll build

```mermaid
flowchart LR
  Schedule[@Schedule 00:15] --> Nightly[NightlyInterestBean.nightlyInterest]
  Nightly --> Run[runOnce REQUIRES_NEW]
  Run --> Guard[(interest_runs PK=run_date)]
  Run --> Accounts[(savings accounts)]

  UI[/reminders servlet/] --> Scheduler[ReminderScheduler @Singleton TimerService]
  Scheduler -->|createCalendarTimer / createSingleActionTimer| Fire[@Timeout onFire]
```

## Idempotency: the key defensive pattern

Timer callbacks can fire **twice** in the real world:

- Node crashed mid-callback, another node picks up the missed timer.
- Container re-tries after a rollback.
- Persistent timer store replays on boot.

Our answer in [`NightlyInterestBean.runOnce`](./src/main/java/org/ejblab/banking/l06/NightlyInterestBean.java):

```java
InterestRun existing = em.find(InterestRun.class, day);
if (existing != null) return;     // someone (or some past us) already did this day
em.persist(new InterestRun(day)); // PK conflict -> we lose the race, and that's fine
```

`run_date` is the PK of the `interest_runs` table - our lock. Two
concurrent fires for the same day: one succeeds, the other's flush
throws a constraint violation and the TX rolls back. Zero double-pay.

## Cluster-safety of the timer store

For true "one fire per cluster", configure the `ejb3` subsystem to use
a JDBC-backed timer store. In CLI:

```cli
/subsystem=ejb3/service=timer-service/database-data-store=ejb-timer-store:add(
    allow-execution=true, datasource-jndi-name=java:jboss/datasources/BankingDS,
    partition=ejb-timer-partition, refresh-interval=60000)
/subsystem=ejb3/service=timer-service:write-attribute(name=default-data-store, value=ejb-timer-store)
```

Without this, every node sees its own timers and you get duplicate
fires in HA setups.

## Run it

```bash
docker compose -f ../docker/docker-compose.yml up -d postgres
mvn -q clean wildfly:package wildfly:dev

# Trigger the interest job now (don't wait until 00:15):
curl -X POST http://localhost:8080/banking-lesson-06-timers/run-interest

# Create a single-action reminder 10s in the future:
curl -X POST 'http://localhost:8080/banking-lesson-06-timers/reminders?message=hello&inSeconds=10'

# List active timers:
curl 'http://localhost:8080/banking-lesson-06-timers/reminders'
```

## Pitfalls & anti-patterns

1. **Non-idempotent callback.** Two fires credit interest twice. Always
   lock by a natural idempotency key (date, clientRequestId, etc.).

2. **Runtime exception in a persistent timer callback.** The container
   retries. Without idempotency, the retry causes exactly the bug above.

3. **Long-running callback.** Timers run on a small thread pool; a
   30-minute job starves other scheduled tasks. Offload to an
   `@Asynchronous` method or to a JMS queue (Lesson 7).

4. **Assuming `@Schedule` timers survive a class rename.** Persistent
   timers are bound to the bean class name. Rename = orphaned timers.
   Deploy with `persistent-timer-management` cleanup.

5. **`@AroundInvoke` interceptor silently skipped on timer callbacks.**
   Use `@AroundTimeout` from Lesson 5 to observe timer fires.

6. **`ScheduleExpression` Friday/Saturday gotchas.** `dayOfWeek` defaults
   to `*` (always). Don't assume "weekdays" by omission; be explicit:
   `dayOfWeek="Mon-Fri"`.

## Interview Q&A

**Q1. What's the difference between a persistent and non-persistent timer?**
A. Persistent = stored in WildFly's timer store (file/JDBC), survives
JVM restart, and in a cluster is "leased" so exactly one node fires
it. Non-persistent = in-memory only, per-node, lost on restart.

**Q2. The callback threw an exception. What does the container do?**
A. For persistent timers, it retries once by default and then, if still
failing, suspends the timer. For non-persistent, it fires next on the
next schedule iteration. You should always log+rethrow in timer
callbacks so the retry actually happens.

**Q3. How do you pick between EJB Timer Service, Quartz, and
`ScheduledExecutorService`?**
A. EJB Timer: integrated with container (TX, security, JTA), cluster
semantics with a shared timer store, portable. Quartz: richer
scheduling features (calendars, priorities, misfire policies), larger
dependency. `ScheduledExecutorService`: zero ceremony, but no HA, no
TX integration, you own failover. Most EE apps should use EJB Timer
for anything that touches the DB.

**Q4. How would you cancel a dynamically created timer from a REST call?**
A. Persist a stable id alongside the timer's `info` payload. On the
cancel endpoint, iterate `timerService.getTimers()`, match on id,
call `cancel()`. See [`ReminderScheduler.cancel`](./src/main/java/org/ejblab/banking/l06/ReminderScheduler.java).

**Q5. Two nodes, persistent timer, one node fails mid-callback. Is the
work lost?**
A. Depends. With a file-based timer store, the other node doesn't see
the missed fire - work is lost until the failed node restarts. With a
shared JDBC timer store, the other node picks it up on the next refresh
and re-fires - which is why idempotency is non-negotiable.

## What's next

[Lesson 7 - JMS + MDBs](../banking-lesson-07-jms-mdb): move async work
off timers and onto a real message queue with retry and DLQ.
