package org.ejblab.banking.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO crossing bean and messaging boundaries. Immutable record so it's
 * obviously safe to send over JMS.
 */
public record TransferRequest(
        String clientRequestId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount) implements Serializable {

    public static TransferRequest of(String from, String to, BigDecimal amount) {
        return new TransferRequest(UUID.randomUUID().toString(), from, to, amount);
    }
}
