package org.ejblab.banking.l09.api;

import jakarta.ejb.Remote;

import java.math.BigDecimal;
import java.util.List;

/**
 * Remote view exposed via WildFly's {@code http-remoting} transport. Both the
 * server and every client depend on this same interface JAR — the classic
 * "shared API" contract.
 *
 * <p>DTOs returned from remote methods MUST be {@link java.io.Serializable}.
 * Prefer records (serializable by default if all components are) or classes
 * with explicit {@code serialVersionUID} so wire compatibility is stable.
 */
@Remote
public interface AccountQuery {

    /** All account numbers in the system. Serializable by construction. */
    List<String> listAccountNumbers();

    /** Balance for the given account number, or {@code null} if unknown. */
    BigDecimal balanceOf(String accountNumber);

    /** Small heartbeat endpoint for connectivity tests. */
    String ping(String message);
}
