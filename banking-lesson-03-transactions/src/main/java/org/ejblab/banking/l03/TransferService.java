package org.ejblab.banking.l03;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.LedgerEntry;
import org.ejblab.banking.domain.Transfer;

import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * A banking {@code TransferService} that demonstrates every
 * {@link TransactionAttributeType}.
 *
 * <p>Defaults: a bean without any {@code @TransactionAttribute} is
 * {@code REQUIRED}. We annotate everything explicitly here to make the
 * semantics obvious.
 *
 * <h2>Rollback rules recap</h2>
 * <ul>
 *   <li>Unchecked ({@link RuntimeException}) thrown from a CMT method =&gt;
 *       container rolls back the TX.</li>
 *   <li>Checked exception thrown from a CMT method =&gt; container
 *       commits the TX (!) unless the exception is annotated
 *       {@link jakarta.ejb.ApplicationException}{@code (rollback=true)}.</li>
 *   <li>{@link EJBContext#setRollbackOnly()} forces rollback regardless
 *       of exception behavior.</li>
 * </ul>
 */
@Stateless
public class TransferService {

    private static final Logger log = Logger.getLogger(TransferService.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @Inject
    AccountRepository accounts;

    @Resource
    SessionContext ctx;  // used to break the self-invocation pitfall

    // ---------------------------------------------------------------------
    // The everyday case: REQUIRED (join existing TX, or start one).
    // ---------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transfer transfer(String from, String to, BigDecimal amount) {
        Account src = accounts.findByNumberForUpdate(from);
        Account dst = accounts.findByNumberForUpdate(to);
        src.debit(amount);   // may throw InsufficientFundsException (runtime, @AppException rollback)
        dst.credit(amount);

        Transfer t = new Transfer(src, dst, amount);
        em.persist(t);
        em.persist(new LedgerEntry(src, t, LedgerEntry.Direction.DEBIT, amount));
        em.persist(new LedgerEntry(dst, t, LedgerEntry.Direction.CREDIT, amount));
        t.markCompleted();
        return t;
    }

    // ---------------------------------------------------------------------
    // REQUIRES_NEW: always start a fresh TX. Useful for audit trails you
    // want to keep even if the outer TX rolls back.
    // ---------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void auditLog(String message) {
        log.info("AUDIT: " + message);
        // In a real app, we'd persist an AuditEvent entity here so that
        // even if the caller's TX rolls back, this audit row survives.
    }

    // ---------------------------------------------------------------------
    // MANDATORY: caller MUST already have a TX, else an EJBTransactionRequiredException is thrown.
    // ---------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void postLedger(LedgerEntry entry) {
        em.persist(entry);
    }

    // ---------------------------------------------------------------------
    // NEVER: caller MUST NOT have a TX, else EJBException.
    // Niche use: long-running cleanup scripts that must not block a TX.
    // ---------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public String describeWithoutTx() {
        return "no TX allowed here";
    }

    // ---------------------------------------------------------------------
    // NOT_SUPPORTED: suspends the caller's TX (if any), runs un-transacted.
    // Useful for expensive read-only work you don't want to lock up resources for.
    // ---------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long countAccountsWithoutTx() {
        return em.createQuery("SELECT COUNT(a) FROM Account a", Long.class).getSingleResult();
    }

    // ---------------------------------------------------------------------
    // SUPPORTS: joins if a TX exists, otherwise runs un-transacted.
    // Rarely what you want - behavior depends on the caller. Avoid unless
    // you have a good reason.
    // ---------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Account lookup(String accountNumber) {
        return accounts.findByNumber(accountNumber).orElseThrow();
    }

    // =====================================================================
    // The self-invocation pitfall.
    // =====================================================================

    /**
     * BUG: call to {@code this.auditLog(...)} bypasses the EJB proxy.
     * Even though {@code auditLog} is annotated {@link TransactionAttributeType#REQUIRES_NEW},
     * it ends up running in the CURRENT TX (or in no TX if there is none).
     *
     * <p>When the outer TX rolls back, the "audit" goes with it. Not good.
     */
    public void transferBuggyAudit(String from, String to, BigDecimal amount) {
        Transfer t = transfer(from, to, amount);
        this.auditLog("transfer " + t.getClientRequestId());    // <-- BYPASSES PROXY
    }

    /**
     * FIX: ask the container for our own business object via the proxy.
     * The call goes through the interceptor chain, so {@code REQUIRES_NEW}
     * is honoured and the audit survives rollback of the outer TX.
     */
    public void transferWithCorrectAudit(String from, String to, BigDecimal amount) {
        TransferService self = ctx.getBusinessObject(TransferService.class);
        Transfer t = self.transfer(from, to, amount);
        self.auditLog("transfer " + t.getClientRequestId());
    }
}
