package org.ejblab.banking.l10;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.AccountType;
import org.ejblab.banking.domain.Customer;

import java.math.BigDecimal;

@Startup
@Singleton
@DependsOn("MigrationsBean")
public class SeedBean {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void seed() {
        Long count = em.createQuery("select count(c) from Customer c", Long.class).getSingleResult();
        if (count > 0) return;

        Customer customer = new Customer();
        customer.setFullName("E2E Ethan");
        customer.setEmail("ethan.e2e@example.com");
        em.persist(customer);

        em.persist(new Account("E2E-001", customer, AccountType.CHECKING, new BigDecimal("1000.00")));
        em.persist(new Account("E2E-002", customer, AccountType.SAVINGS, new BigDecimal("500.00")));
    }
}
