# Lesson 9 — Remote EJB (over http-remoting)

## Why this exists

EJB isn't just in-process. WildFly's **http-remoting** transport lets other JVMs — standalone apps, other WildFlys, Spring Boot, Quarkus, batch jobs — call your EJBs over HTTP/1.1 (or HTTP/2) with JBoss Marshalling payloads. It's the modern replacement for the old JNP/Remoting 3 TCP transports.

This lesson packages **three** modules so you see the whole wire:

```
banking-lesson-09-remote-ejb/
├── api/        shared @Remote interface JAR (depends only on jakarta.ejb-api)
├── server/     WAR deployed to WildFly that implements the interface
└── client/     standalone Java app that looks up + invokes the bean
```

## The contract

```java
@Remote
public interface AccountQuery {
    List<String> listAccountNumbers();
    BigDecimal   balanceOf(String accountNumber);
    String       ping(String message);
}
```

All arguments and return types MUST be `Serializable`. Prefer records / DTOs with an explicit `serialVersionUID`.

## JNDI lookup name

```
ejb:/banking-lesson-09-remote-ejb-server/AccountQueryBean!org.ejblab.banking.l09.api.AccountQuery
```

Pattern (WAR): `ejb:/<war-name>/<bean-class>!<fully-qualified-interface>`

If the bean were in an EAR: `ejb:<app>/<module>/<distinct>/<bean>!<iface>`.

## Two ways to configure the client

### 1. Programmatic (`RemoteClient.java`)

```java
Hashtable<String,Object> env = new Hashtable<>();
env.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
env.put(Context.PROVIDER_URL, "remote+http://localhost:8080");
env.put(Context.SECURITY_PRINCIPAL, "alice");
env.put(Context.SECURITY_CREDENTIALS, "alice123");
env.put("jboss.naming.client.ejb.context", true);
new InitialContext(env).lookup("ejb:/banking-lesson-09-remote-ejb-server/AccountQueryBean!...");
```

### 2. File-based (`wildfly-config.xml` on classpath)

Declarative, good for ops:

```xml
<jboss-ejb-client xmlns="urn:jboss:wildfly-client-ejb:3.0">
    <connections>
        <connection uri="remote+http://node1:8080"/>
        <connection uri="remote+http://node2:8080"/>  <!-- failover -->
    </connections>
    <invocation-timeout seconds="5"/>
</jboss-ejb-client>
```

## Failover & clustering

The `wildfly-client-ejb` config supports **multiple connections**. The default first-available strategy means that when node1 is down, the invocation retries against node2 transparently. For Stateful EJBs, session affinity piggybacks on a `SFSB-Session-ID` cookie — losing a node loses the session unless you front the cluster with a shared cache (Infinispan).

Add `<cluster name="ejb"/>` in the config when you want invocation-level load balancing across the whole cluster (discovered via Infinispan, not hand-listed):

```xml
<jboss-ejb-client xmlns="urn:jboss:wildfly-client-ejb:3.0">
    <default-cluster name="ejb"/>
    <clusters>
        <cluster name="ejb" max-allowed-connected-nodes="5"/>
    </clusters>
</jboss-ejb-client>
```

## Pitfalls

1. **ClassCastException at the client**. The client must have the exact same `AccountQuery` interface (same package/name) on its classpath. Ship the API JAR — never copy-paste the interface.
2. **`NamingException: jboss.naming.client.ejb.context`** missing. Without it, the client thinks you want plain remote naming, not EJB invocations. Always set it or use `wildfly-config.xml`.
3. **Serialization filters**. Java 17+ enforces JEP 290 filters. On the *server* add `-Djdk.serialFilter='!*;org.ejblab.**'` to whitelist the API package and deny the rest.
4. **Timeouts hidden by retries**. If `invocation-timeout` isn't set, a silent network partition can stall a thread for minutes. Always set a finite timeout and handle the `RequestSendFailedException`.
5. **Auth propagation**. The *client* supplies credentials; the *server* maps them via its security domain. Alice's password must exist in the server's Elytron realm — reuse Lesson 8's `sec_users` table.
6. **Stateful sticky failover is best-effort**. The ONLY way to survive node loss mid-conversation is session replication (Infinispan) or designing the bean to be idempotent (rebuildable state).
7. **`remote://` vs `remote+http://`**. The former is the legacy Remoting 3 scheme (dedicated port 8080 via socket binding `remoting`) and is deprecated; always use `remote+http://` (multiplexed on port 8080).

## Interview Q&A

**Q: Why not just REST?**
A: Remote EJB preserves full transactional propagation (JTA/XA), security context, and exception types across process boundaries — things REST doesn't give you for free. For service-to-service calls within a single trusted boundary it's still faster and more expressive than JSON-over-HTTP.

**Q: Is classic RMI/IIOP still relevant?**
A: Not for WildFly. IIOP is gone from the default `standalone.xml`. Use http-remoting for new code. Only legacy CORBA bridges still need IIOP.

**Q: What's the performance cost of a remote call?**
A: Typically 0.5–2 ms round-trip on localhost (marshalling + HTTP/1.1 upgrade + single-thread invocation). Cross-datacenter, the network dominates. Batch reads (return a List, not one call per ID) is the #1 optimization.

**Q: Transaction propagation: does it "just work"?**
A: Yes — with a caveat. The client must have JTA (`UserTransaction`) available and the invocation uses distributed txn IDs. Most standalone clients don't start transactions; the server-side CMT handles everything inside its own tx. If you need client-started XA, use `Narayana` as the client TM.

**Q: Can a remote client do CDI injection of the proxy?**
A: Only in-container. A plain `main()` uses JNDI lookup. A servlet in another WildFly module CAN inject via `@EJB(lookup="ejb:/.../AccountQueryBean!...")`.

## Run locally

```bash
# 1. Database up
docker compose -f docker/docker-compose.yml up -d

# 2. Build API + server + client
./mvnw -pl banking-lesson-09-remote-ejb -am install

# 3. Provision + deploy the server
./mvnw -pl banking-lesson-09-remote-ejb/server wildfly:provision wildfly:deploy

# 4. Sanity-check local endpoint
curl http://localhost:8080/banking-lesson-09-remote-ejb-server/accounts

# 5. Run the standalone client
java -jar banking-lesson-09-remote-ejb/client/target/banking-lesson-09-remote-ejb-client-*.jar
```

Expected output:

```
ping -> pong: hi
accounts -> [REMOTE-001, REMOTE-002]
balance REMOTE-001 -> 500.00
```
