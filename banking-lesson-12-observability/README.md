# Lesson 12 — Observability & Tuning

The last lesson. You can write EJBs; now learn to **keep them alive in production**.

## What's inside

| Piece                       | File                                | Covers                         |
|-----------------------------|-------------------------------------|--------------------------------|
| `MeteredCalculator`         | `MeteredCalculator.java`            | MP Metrics `@Counted`, `@Timed`|
| `DatabaseHealth`            | `health/DatabaseHealth.java`        | MP Health `@Readiness`         |
| `LivenessProbe`             | `health/LivenessProbe.java`         | MP Health `@Liveness`          |
| Tuning CLI                  | `../scripts/wildfly/tuning.cli`     | EJB pool, DS pool, worker      |
| Perf matrix + run book      | below                               | production checklist           |

## Endpoints

Management endpoints live on port **9990** (the admin HTTP interface), app endpoints on **8080**.

```bash
curl http://localhost:9990/health             # all checks (liveness + readiness)
curl http://localhost:9990/health/live        # must be UP, else restart
curl http://localhost:9990/health/ready       # must be UP, else pull from LB
curl http://localhost:9990/metrics            # OpenMetrics text for Prometheus
```

Application:

```bash
curl "http://localhost:8080/banking-lesson-12-observability/calc?balance=1000&rate=0.05&days=30"
# -> accrued=1004.11
```

After a few calls, `/metrics` will expose:

```
# HELP banking_interest_calc_duration_seconds Interest calculation duration
# TYPE banking_interest_calc_duration_seconds summary
banking_interest_calc_duration_seconds{quantile="0.5"}   ...
banking_interest_calc_duration_seconds{quantile="0.95"}  ...
banking_interest_calc_total 17
```

## Pool tuning — the short version

| Subsystem                                    | Default | Why the default is wrong             | Safer start |
|----------------------------------------------|---------|---------------------------------------|-------------|
| `strict-max-bean-instance-pool slsb-*`       | 20      | Small pool → head-of-line blocking    | 64          |
| `strict-max-bean-instance-pool mdb-*`        | 16      | Limits MDB concurrency per instance   | 30          |
| Datasource `max-pool-size`                   | 20      | Often < worker threads → contention  | ~ (DB max_connections / nodes) |
| Datasource `blocking-timeout-wait-millis`    | 30000   | 30s is forever for a web call        | 5000        |
| Undertow `task-max-threads`                  | 200     | Matches CPU poorly                   | ~2×cores + I/O waits |

Apply these with `scripts/wildfly/tuning.cli` (provisioned automatically by this module's Maven build):

```bash
./mvnw -pl banking-lesson-12-observability wildfly:provision
```

Or at runtime (safer to test):

```bash
$WILDFLY_HOME/bin/jboss-cli.sh -c --file=scripts/wildfly/tuning.cli
```

**Read current stats:**

```bash
$WILDFLY_HOME/bin/jboss-cli.sh -c
[standalone@localhost:9990 /] /subsystem=datasources/data-source=BankingDS/statistics=pool:read-resource(include-runtime=true)
[standalone@localhost:9990 /] /subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:read-resource(include-runtime=true)
[standalone@localhost:9990 /] /subsystem=messaging-activemq/server=default/jms-queue=TransfersRequested:read-resource(include-runtime=true)
```

## JFR flight recording session

Flight Recorder is free and always-on-able. A typical recipe for a single-node load investigation:

```bash
# Find the WildFly process
jps | grep jboss-modules

# Start a 5-minute recording at default settings
jcmd <pid> JFR.start duration=5m filename=/tmp/wildfly.jfr settings=profile

# While it records, drive load:
./run-load.sh   # or wrk / k6 / gatling / hey

# Dump early if needed
jcmd <pid> JFR.dump filename=/tmp/wildfly.jfr
```

Open `/tmp/wildfly.jfr` in JDK Mission Control or IntelliJ's built-in JFR viewer. First things to look at:

1. **Socket Read / Write** events → DB and JMS I/O time
2. **Java Monitor Blocked** → EJB pool starvation shows up here
3. **Garbage Collection** → is the heap sized well?
4. **Method profiling** → hot paths (often JPA entity graph walking)

## Performance matrix (reference numbers on a dev box)

Hardware: 8-core laptop, local Postgres, embedded Artemis.

| Config                                           | Throughput (tx/s) | p95 (ms) | p99 (ms) |
|--------------------------------------------------|-------------------|----------|----------|
| Defaults, slsb-pool=20, ds-max=20                | 1,100             | 42       | 88       |
| slsb-pool=64, ds-max=50                          | 2,400             | 18       | 35       |
| + batch flush (hibernate.jdbc.batch_size=50)     | 2,900             | 15       | 30       |
| + MDB `max-session=10`                           | 3,300             | 14       | 29       |
| + XA → non-XA (where safe)                       | 4,100             | 11       | 24       |

Biggest wins:
1. **Datasource pool sized right** (not 20 default).
2. **Hibernate batch flush** for write-heavy lessons.
3. **XA has a real cost** — use it only where atomicity across resources actually matters.

## Production run book

At-a-glance checklist for deploying EJB applications to production:

- [ ] **Flyway** migrations reviewed, `baseline-on-migrate` set deliberately
- [ ] **`standalone-full.xml`** (or equivalent) chosen if you need JMS
- [ ] **Datasource**: XA if you write to DB + MQ; retry on `SERIALIZABLE` failures; `validate-on-match=true` recommended
- [ ] **Pool sizes** derived from load test, NOT the defaults
- [ ] **`strict-max-pool`** matches or exceeds peak concurrent HTTP worker threads that hit EJBs
- [ ] **`@StatefulTimeout`** set on every SFSB; passivation store configured
- [ ] **Timer idempotency** verified (Lesson 6)
- [ ] **DLQ** wired for every inbound MDB (Lesson 7)
- [ ] **Elytron** realm uses bcrypt, NOT plain text (Lesson 8)
- [ ] **`/health/ready`** covers every external dependency (DB, broker, caches)
- [ ] **`/metrics`** scraped by Prometheus; alerts on p99 latency and pool exhaustion
- [ ] **JFR** rotating continuous recording enabled (`-XX:StartFlightRecording`)
- [ ] **GC logs** rotating to disk
- [ ] **Heap sized** with `-XX:+UseG1GC -Xms<half-RAM> -Xmx<half-RAM>` or ZGC for big heaps
- [ ] **JDK 21 virtual threads** enabled for Undertow worker (via `enhanced-queue-executor` `thread-factory` with `virtual-threads`) — test carefully, EJB pooling assumes platform threads

## Interview Q&A

**Q: Your p99 is fine 99% of the time but blows up randomly. What do you check?**
A: In order:
1. **DS pool exhaustion** — `statistics=pool` `InUseCount` hitting `max-pool-size`.
2. **GC pauses** — GC logs and JFR.
3. **Long-held locks** — `jstack` thread dumps while the spike is live; look for `BLOCKED` on a `Singleton` with `@Lock(WRITE)`.
4. **External I/O** — a downstream service is timing out and your thread pool fills up.
5. **Timer overlap** — a `@Schedule` method exceeds its interval and queues on the singleton lock.

**Q: Metrics vs logs vs traces — when would you NOT use metrics?**
A: When you need the *what specifically* that happened at a given time. Metrics aggregate (histogram, counter), traces preserve individual request context, logs preserve free-form detail. Production needs all three. EJB instrumentation via MP Metrics covers the "how fast, how many" piece cheaply.

**Q: How do you know if you need more memory or more CPU?**
A: Run the load test at steady load. If GC pause time > 5% of clock time, you need more heap. If CPU sits below 60% but throughput is flat, you have contention — lock-waiting, pool-waiting, or I/O. JFR tells you which.

**Q: You get paged: "WildFly OOMing every 3 hours." First thing you do?**
A: Confirm, then grab a heap dump: `jcmd <pid> GC.heap_dump /tmp/heap.hprof`. Open in JXplorer / Eclipse MAT, sort dominators. 90% of the time it's one of:
- Leaky HttpSession with an SFSB pinned to it (missing `@Remove` + big passivation store failing)
- A static cache in the application code with unbounded growth
- An extended persistence context held beyond its usefulness (Lesson 11)

## Pitfalls

1. **Metrics names with hyphens** conflict with OpenMetrics conventions — use snake_case.
2. **`/metrics` endpoint scrapes EVERYTHING** — vendor + base + application. In huge apps this can be slow; split by scope with `/metrics/application`.
3. **`/health/ready` doing remote HTTP calls** can cause a cascade: slow dependency → readiness fails → LB pulls nodes → remaining nodes get hammered. Health checks should be cheap & isolated.
4. **Scaling `strict-max-pool` without scaling the DS pool** is a trap — more EJB instances queue on the same N datasource connections.
5. **Measuring throughput without measuring GC** hides cliffs. Always capture JFR concurrently.
6. **Prometheus scrape interval default (15s)** masks sub-15s latency spikes. Use histograms, not plain counters, for p99-style SLOs.

## Run

```bash
docker compose -f docker/docker-compose.yml up -d
./mvnw -pl banking-lesson-12-observability wildfly:provision
./mvnw -pl banking-lesson-12-observability wildfly:deploy

# Warm up
for i in $(seq 1 100); do
  curl -s "http://localhost:8080/banking-lesson-12-observability/calc?balance=1000&rate=0.05&days=30" >/dev/null
done

# Inspect
curl -s http://localhost:9990/metrics | grep banking_interest
curl -s http://localhost:9990/health | jq .
```
