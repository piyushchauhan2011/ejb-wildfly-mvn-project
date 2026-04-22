# EJB Mastery - Banking Lab

A hands-on, 12-lesson curriculum for mastering **Jakarta EE EJB on WildFly**,
from a quick refresher through production-grade patterns. One growing Maven
multi-module project, one realistic banking domain (accounts, transfers,
ledger, notifications), one pragmatic goal: **excel in interviews and on
the job**.

## Tech stack

| Layer           | Choice                                                            |
| --------------- | ----------------------------------------------------------------- |
| Language        | Java 21                                                           |
| Platform        | Jakarta EE 10 (`jakarta.*` namespace)                             |
| App server      | WildFly 36 (provisioned by `wildfly-maven-plugin`)                |
| Persistence     | JPA (Hibernate) + PostgreSQL 16 via Docker                        |
| Messaging       | Embedded Apache Artemis (JMS)                                     |
| Security        | Elytron + jdbc-realm                                              |
| Web             | Jakarta Servlet, JSP (intentionally - EJB + Servlet is the theme) |
| Build           | Maven 3.9+                                                        |
| Tests           | JUnit 5 + Arquillian (managed WildFly) + Testcontainers           |

## Curriculum

| # | Module | Focus |
|---|--------|-------|
| 00 | [`banking-domain`](./banking-domain) | Shared JPA entities + DTOs + exceptions |
| 01 | [`banking-lesson-01-refresher`](./banking-lesson-01-refresher) | `@Stateless`, `@Stateful`, `@Singleton`, JNDI, Servlet front door |
| 02 | [`banking-lesson-02-jpa-cmt`](./banking-lesson-02-jpa-cmt) | JPA + Container-Managed Transactions, Flyway, Postgres |
| 03 | [`banking-lesson-03-transactions`](./banking-lesson-03-transactions) | TX attributes, BMT, rollback rules, self-invocation, SERIALIZABLE retry |
| 04 | [`banking-lesson-04-concurrency`](./banking-lesson-04-concurrency) | `@Lock`, `@Asynchronous`, `ManagedExecutorService`, rate limiter |
| 05 | [`banking-lesson-05-interceptors`](./banking-lesson-05-interceptors) | `@AroundInvoke`, `@AroundTimeout`, custom `@Audited` binding |
| 06 | [`banking-lesson-06-timers`](./banking-lesson-06-timers) | Declarative + programmatic timers, persistent vs non-persistent |
| 07 | [`banking-lesson-07-jms-mdb`](./banking-lesson-07-jms-mdb) | MDBs, Artemis, redelivery, DLQ, sync-vs-async latency |
| 08 | [`banking-lesson-08-security`](./banking-lesson-08-security) | Elytron jdbc-realm, `@RolesAllowed`, `@RunAs`, form login |
| 09 | [`banking-lesson-09-remote-ejb`](./banking-lesson-09-remote-ejb) | `@Remote` over `http-remoting`, standalone + servlet clients |
| 10 | [`banking-lesson-10-end-to-end`](./banking-lesson-10-end-to-end) | Servlet -> EJB facade -> MDB -> JPA -> JMS -> Timer |
| 11 | [`banking-lesson-11-advanced-patterns`](./banking-lesson-11-advanced-patterns) | Stateful passivation, extended PC, `@Version` + retry, bean validation |
| 12 | [`banking-lesson-12-observability`](./banking-lesson-12-observability) | MP Metrics/Health, pool tuning, JFR, production run book |

Each lesson ships a full `README.md` with:

- **Concepts** + sequence diagram
- **Code walkthrough** (what to read first, what to run)
- **Pitfalls & anti-patterns** (the stuff that bites in production)
- **Interview Q&A** (real questions, model answers)
- **Benchmark notes** (with a tiny harness where applicable)

## Quick start

### 1. Start Postgres

```bash
docker compose -f docker/docker-compose.yml up -d postgres
```

### 2. Build everything

```bash
mvn -q -DskipITs clean install
```

### 3. Run any lesson

`wildfly:dev` provisions WildFly, starts it, deploys the lesson as an
**exploded** webapp, and watches `src/main` for changes. Because an exploded
deployment and a packaged `.war` file cannot coexist in `target/`, always
start from a clean target:

```bash
cd banking-lesson-01-refresher
mvn -q clean wildfly:dev
# App at http://localhost:8080/banking-lesson-01-refresher/
# Ctrl+C to stop; the provisioned server lives in target/server/
```

If you only want to boot the server and deploy once (no file watcher):

```bash
mvn -q clean wildfly:run
```

> **Always prefix with `clean`** when switching between packaged and
> dev-mode builds. The packaged `.war` **file** (from `mvn install`/`verify`)
> and the exploded `.war/` **directory** (from `wildfly:dev`/`wildfly:run`)
> share the same path in `target/`, so whichever ran last poisons the other:
>
> - `… .war/WEB-INF/beans.xml: Not a directory` → leftover packaged file,
>   run `mvn clean wildfly:dev`.
> - `… .war isn't a file` → leftover exploded dir, run `mvn clean verify`.

### 4. Run the Arquillian integration tests for a lesson

ITs run by default on `verify`. Start from a clean target so the packaged
war can be built (see warning above):

```bash
mvn -q clean verify
```

Run all lessons' ITs in one shot from the repo root:

```bash
mvn -q clean verify
```

To skip ITs during a fast rebuild, pass `-DskipITs`:

```bash
mvn -q -DskipITs clean install
```

## Conventions

- **Package root**: `org.ejblab.banking`
- **Shared JNDI**: `java:jboss/datasources/BankingDS` (JTA), `java:jboss/datasources/BankingXADS` (XA)
- **Default JMS queues**: `java:/jms/queue/TransfersRequested`, `TransfersCompleted`, `TransfersDLQ`
- **Integration tests**: `mvn verify` runs every lesson's Arquillian IT; pass `-DskipITs` to skip them (e.g. `mvn -DskipITs clean install` for fast local dev)

## Directory layout

```
.
├── banking-domain/                  shared entities + DTOs
├── banking-lesson-01-refresher/     L1: refresher
├── ...
├── banking-lesson-12-observability/ L12: observability + tuning
├── docker/
│   └── docker-compose.yml           Postgres + pgAdmin (profile `tools`)
├── scripts/
│   └── wildfly/                     CLI scripts (datasources, JMS, security)
└── docs/                            cheatsheets, diagrams
```

## Interview-ready EJB cheatsheet

See [`docs/interview-cheatsheet.md`](./docs/interview-cheatsheet.md) for the
condensed "one-pager" you can review the morning of an interview.
