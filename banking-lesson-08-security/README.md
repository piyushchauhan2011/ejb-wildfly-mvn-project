# Lesson 8 — Security with Elytron

## Objective

Wire **Elytron** into WildFly so that:

1. A `jdbc-realm` authenticates users against Postgres (`sec_users` / `sec_user_roles`).
2. A servlet front door authenticates the caller (BASIC or FORM) and **propagates identity** into EJBs automatically.
3. EJBs authorize with `@RolesAllowed`, `@PermitAll`, `@DenyAll`, `@DeclareRoles`, and `@RunAs`.
4. `EJBContext.getCallerPrincipal()` and `HttpServletRequest.isUserInRole(...)` both return the right thing.

## Concept map

```mermaid
flowchart LR
  Browser -->|BASIC/FORM| Servlet
  Servlet -->|SecurityIdentity propagated| TransferFacade
  TransferFacade -->|@RunAs SYSTEM| AuditService
  subgraph Elytron
    Realm[jdbc-realm Postgres] --> Domain[BankingDomain]
    Domain --> HttpAuth[http-authentication-factory]
    Domain --> EjbAuth[application-security-domain ejb3]
  end
```

## What's set up

**`scripts/wildfly/security-elytron.cli`** (provisioned by the `wildfly-maven-plugin`) creates:

- `BankingJdbcRealm` — queries `sec_users` / `sec_user_roles`
- `BankingDomain` — Elytron security domain wrapping the realm
- `banking-http-auth` — BASIC + FORM authentication factory
- Undertow + EJB3 application security domains named `BankingDomain`

The WAR chooses its domain with `WEB-INF/jboss-web.xml` (`<security-domain>BankingDomain</security-domain>`) and the EJBs do the same via `WEB-INF/jboss-ejb3.xml`.

**Migration V2** seeds:

| user  | password  | role     |
|-------|-----------|----------|
| alice | alice123  | TELLER   |
| bob   | bob123    | CUSTOMER |
| carol | carol123  | AUDITOR  |

> ⚠️ Plain-text passwords — for the lesson only. Production: use **bcrypt** (`password-type="bcrypt"`) or modular-crypt.

## Endpoints

| Path        | Auth              | Allowed roles                 |
|-------------|-------------------|--------------------------------|
| `POST /transfer` | BASIC        | TELLER, CUSTOMER               |
| `GET /terms`     | BASIC        | any authenticated (`@PermitAll` on EJB) |
| `GET /who`       | BASIC        | any authenticated             |
| `GET /logout`    | BASIC        | any authenticated             |

Try it:

```bash
curl -u alice:alice123 -X POST --data "from=A&to=B&amount=10" \
  http://localhost:8080/banking-lesson-08-security/transfer
# -> ok

curl -u bob:bob123 http://localhost:8080/banking-lesson-08-security/who
# -> user=bob TELLER?=false CUSTOMER?=true AUDITOR?=false

curl -u carol:carol123 -X POST --data "from=A&to=B&amount=10" \
  http://localhost:8080/banking-lesson-08-security/transfer
# -> forbidden (AUDITOR can't initiate transfers)
```

## `@RunAs` demo

`TransferFacade` is annotated `@RunAs("SYSTEM")`. That means when it calls `AuditService` (which requires `AUDITOR` or `SYSTEM`), the downstream EJB sees the principal as `SYSTEM`, **not** as `alice`.

Without `@RunAs`, the call would throw `EJBAccessException` because `alice` isn't an auditor. This is the textbook pattern for "system actions executed on behalf of a user": the user's identity is captured in the audit payload, but the call itself uses an elevated, **well-defined** system role.

## Interview Q&A

**Q: Does Elytron replace legacy security-domain `other`?**
A: Yes. Since WildFly 25+ Elytron is the default. The legacy PicketBox subsystem is removed. All new configs should target `elytron` + `application-security-domain` mappings for `undertow` and `ejb3`.

**Q: Where does role propagation happen — web container or EJB container?**
A: Both. The web container authenticates and builds a `SecurityIdentity`. Elytron's EJB integration picks up the same identity from the invocation context, so `@RolesAllowed` on EJBs works without any manual `pushIdentity`.

**Q: Difference between `@PermitAll` and no annotation at all?**
A: Without an annotation, the **unchecked** default applies — typically "deny all" if a class-level `@RolesAllowed` is present, otherwise effectively permit. `@PermitAll` is **explicit** and safer. Always annotate.

**Q: How do `@RunAs` and `@RolesAllowed` interact?**
A: `@RunAs` changes the principal **seen by callees**. It does not bypass the caller's own `@RolesAllowed` check — the caller still has to be authorized to enter the method. Only outgoing calls see the run-as principal.

**Q: Can I call `getCallerPrincipal()` in a `@PostConstruct`?**
A: On a Stateless bean — no (there's no caller yet). On a Stateful with `@PostActivate` / `@PrePassivate` — use `EJBContext`, but still: no caller principal during post-construct. Defer caller-sensitive work to the first business method.

## Pitfalls

1. **Role name mismatch**. The role name in `@RolesAllowed("TELLER")` must match the role stored in `sec_user_roles.role` **exactly** (case-sensitive). No automatic mapping.
2. **Forgetting `WEB-INF/jboss-web.xml`**. Without it, WildFly uses the default domain (`other`) and your realm is ignored. Form login "works" but no one has any roles.
3. **`login-config` mismatch with servlet security**. Mixing `@HttpConstraint` with `web.xml` `<security-constraint>` often duplicates or contradicts. Pick one; this lesson uses annotations.
4. **CSRF is still your problem**. Elytron handles authentication and authorization; CSRF protection for state-changing `POST`s is application code.
5. **`@RunAs` is not "sudo for users"**. It swaps to a static role, not to "admin whoever called me". Don't treat it as impersonation.
6. **`@DenyAll` on a superclass** silently denies subclass methods. If you inherit from a framework base class, annotate the subclass methods explicitly.
7. **Plain-text `password-type`** for the realm is fine for lessons, never for prod. Switch to `password-type="bcrypt"` and regenerate the hashes.

## Benchmark notes

- Authentication itself is ~sub-ms once the realm cache is warm; the hot path is usually a single indexed query to `sec_users` + one to `sec_user_roles`.
- Enabling Elytron audit (`core-service=management/access=audit`) costs extra I/O — for very hot paths, route audits via an async queue (Lesson 7) or only log on state-changing calls.
- `@RolesAllowed` checks are O(roles) — trivial compared with JDBC / JMS work.

## Run locally

```bash
docker compose -f docker/docker-compose.yml up -d
./mvnw -pl banking-lesson-08-security wildfly:provision
./mvnw -pl banking-lesson-08-security wildfly:deploy
```
