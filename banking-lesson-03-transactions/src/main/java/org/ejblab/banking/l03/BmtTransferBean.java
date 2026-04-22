package org.ejblab.banking.l03;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.LedgerEntry;
import org.ejblab.banking.domain.Transfer;

import java.math.BigDecimal;

/**
 * Bean-Managed Transaction equivalent of {@link TransferService#transfer}.
 *
 * <p>Pros: fine-grained control, multiple TXs in one method call.<br>
 * Cons: you own rollback correctness. Forgetting a
 * {@link UserTransaction#rollback() rollback()} on failure leaks a TX.
 *
 * <p>Rule of thumb: default to CMT. Only use BMT when you need to span
 * multiple short TXs inside one method (e.g., a bulk importer that
 * commits every 1000 rows to keep locks short).
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BmtTransferBean {

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @Resource
    UserTransaction utx;

    public Transfer transfer(String from, String to, BigDecimal amount) throws Exception {
        utx.begin();
        try {
            Account src = em.createQuery(
                    "SELECT a FROM Account a WHERE a.accountNumber = :n", Account.class)
                    .setParameter("n", from).getSingleResult();
            Account dst = em.createQuery(
                    "SELECT a FROM Account a WHERE a.accountNumber = :n", Account.class)
                    .setParameter("n", to).getSingleResult();

            src.debit(amount);
            dst.credit(amount);

            Transfer t = new Transfer(src, dst, amount);
            em.persist(t);
            em.persist(new LedgerEntry(src, t, LedgerEntry.Direction.DEBIT, amount));
            em.persist(new LedgerEntry(dst, t, LedgerEntry.Direction.CREDIT, amount));
            t.markCompleted();

            utx.commit();
            return t;
        } catch (RuntimeException | Error e) {
            safeRollback();
            throw e;
        } catch (Exception e) {
            safeRollback();
            throw e;
        }
    }

    private void safeRollback() {
        try { utx.rollback(); } catch (Exception ignore) {}
    }
}
