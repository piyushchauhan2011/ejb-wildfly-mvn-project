package org.ejblab.banking.l02;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Stateless repository for {@link Customer}.
 *
 * <p>A container-managed {@link EntityManager} is <strong>per-transaction</strong>
 * here (the default {@code TRANSACTION} type). That means each method call
 * gets a new persistence context. Entities loaded in one call are detached
 * by the time the next call begins.
 */
@Stateless
public class CustomerRepository {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    public Customer save(Customer c) {
        if (c.getId() == null) em.persist(c); else c = em.merge(c);
        return c;
    }

    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(em.find(Customer.class, id));
    }

    public Optional<Customer> findByEmail(String email) {
        return em.createQuery("SELECT c FROM Customer c WHERE c.email = :e", Customer.class)
                .setParameter("e", email)
                .getResultStream()
                .findFirst();
    }

    public List<Customer> findAll() {
        return em.createQuery("SELECT c FROM Customer c ORDER BY c.id", Customer.class).getResultList();
    }
}
