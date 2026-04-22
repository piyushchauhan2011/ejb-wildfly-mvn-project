package org.ejblab.banking.domain;

import jakarta.ejb.ApplicationException;

import java.math.BigDecimal;

/**
 * Thrown when an account does not have sufficient balance for a debit.
 *
 * <p>Marked {@code @ApplicationException(rollback = true)} so the container
 * rolls back the enclosing transaction. This is the textbook example of the
 * EJB rollback rule for <em>checked</em> exceptions: checked exceptions do
 * NOT trigger rollback unless annotated, <em>runtime</em> exceptions do.
 * Lesson 3 spends time on this exact nuance.
 */
@ApplicationException(rollback = true)
public class InsufficientFundsException extends RuntimeException {

    private final String accountNumber;
    private final BigDecimal balance;
    private final BigDecimal attempted;

    public InsufficientFundsException(String accountNumber, BigDecimal balance, BigDecimal attempted) {
        super("Insufficient funds on account " + accountNumber
                + " (balance=" + balance + ", attempted=" + attempted + ")");
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.attempted = attempted;
    }

    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getAttempted() { return attempted; }
}
