package org.ejblab.banking.l12.health;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.sql.DataSource;

import java.sql.Connection;

/**
 * {@link Readiness} probe backed by the datasource. Kubernetes (or any
 * readiness-aware orchestrator) hits {@code /health/ready} and we report
 * UP only if the DB connection is actually usable.
 */
@Readiness
@ApplicationScoped
public class DatabaseHealth implements HealthCheck {

    @Resource(lookup = "java:jboss/datasources/BankingDS")
    DataSource ds;

    @Override
    public HealthCheckResponse call() {
        var b = HealthCheckResponse.named("banking-db");
        try (Connection c = ds.getConnection()) {
            boolean ok = c.isValid(1);
            return (ok ? b.up() : b.down()).build();
        } catch (Exception e) {
            return b.down().withData("error", e.getMessage()).build();
        }
    }
}
