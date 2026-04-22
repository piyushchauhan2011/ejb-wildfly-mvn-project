package org.ejblab.banking.l03;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.AccountType;
import org.ejblab.banking.domain.Customer;

import java.math.BigDecimal;

/** Creates / resets two accounts ACC-001 and ACC-002 with known balances. */
@Stateless
public class SeedBean {

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void reset() {
        em.createQuery("DELETE FROM LedgerEntry").executeUpdate();
        em.createQuery("DELETE FROM Transfer").executeUpdate();
        em.createQuery("UPDATE Account a SET a.balance = :b WHERE a.accountNumber IN (:n1, :n2)")
                .setParameter("b", new BigDecimal("1000.00"))
                .setParameter("n1", "ACC-001")
                .setParameter("n2", "ACC-002")
                .executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void seedIfMissing() {
        Long n = em.createQuery(
                "SELECT COUNT(a) FROM Account a WHERE a.accountNumber IN ('ACC-001','ACC-002')",
                Long.class).getSingleResult();
        if (n == 2L) return;

        Customer c = em.createQuery(
                "SELECT c FROM Customer c WHERE c.email = :e", Customer.class)
                .setParameter("e", "l03-seed@banking.lab")
                .getResultStream().findFirst()
                .orElseGet(() -> {
                    Customer nc = new Customer("L03 Seed", "l03-seed@banking.lab");
                    em.persist(nc);
                    return nc;
                });

        if (em.createQuery("SELECT COUNT(a) FROM Account a WHERE a.accountNumber = 'ACC-001'",
                Long.class).getSingleResult() == 0L) {
            em.persist(new Account("ACC-001", c, AccountType.CHECKING, new BigDecimal("1000.00")));
        }
        if (em.createQuery("SELECT COUNT(a) FROM Account a WHERE a.accountNumber = 'ACC-002'",
                Long.class).getSingleResult() == 0L) {
            em.persist(new Account("ACC-002", c, AccountType.CHECKING, new BigDecimal("1000.00")));
        }
    }
}
