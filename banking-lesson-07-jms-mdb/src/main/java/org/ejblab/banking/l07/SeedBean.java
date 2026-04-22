package org.ejblab.banking.l07;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.AccountType;
import org.ejblab.banking.domain.Customer;

import java.math.BigDecimal;

@Stateless
public class SeedBean {

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void seedIfMissing() {
        Customer c = em.createQuery("SELECT c FROM Customer c WHERE c.email = :e", Customer.class)
                .setParameter("e", "l07-seed@banking.lab")
                .getResultStream().findFirst()
                .orElseGet(() -> { var n = new Customer("L07", "l07-seed@banking.lab"); em.persist(n); return n; });
        if (em.createQuery("SELECT COUNT(a) FROM Account a WHERE a.accountNumber = 'L7-001'",
                Long.class).getSingleResult() == 0L) {
            em.persist(new Account("L7-001", c, AccountType.CHECKING, new BigDecimal("1000.00")));
        }
        if (em.createQuery("SELECT COUNT(a) FROM Account a WHERE a.accountNumber = 'L7-002'",
                Long.class).getSingleResult() == 0L) {
            em.persist(new Account("L7-002", c, AccountType.CHECKING, new BigDecimal("1000.00")));
        }
    }
}
