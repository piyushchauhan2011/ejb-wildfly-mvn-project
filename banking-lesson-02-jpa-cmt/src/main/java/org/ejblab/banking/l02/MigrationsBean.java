package org.ejblab.banking.l02;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Runs Flyway migrations at deploy time.
 *
 * <p>Pattern: a {@code @Startup @Singleton} with {@code @PostConstruct} is the
 * idiomatic place to run code <strong>exactly once per JVM at deploy</strong>.
 * That makes it a natural fit for:
 * <ul>
 *   <li>database migrations</li>
 *   <li>cache priming</li>
 *   <li>resource health checks that should fail the deployment</li>
 * </ul>
 *
 * <p>Why {@link TransactionManagementType#BEAN}? Flyway manages its own
 * transactions against the JDBC {@link DataSource}. We do NOT want the
 * container to start a JTA transaction for us in {@code @PostConstruct}.
 *
 * <p>Pitfall: if you throw from {@code @PostConstruct}, the deployment fails.
 * That's usually desired (fail fast on a bad DB), but be intentional about it.
 */
@Startup
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class MigrationsBean {

    private static final Logger log = Logger.getLogger(MigrationsBean.class.getName());

    @Resource(lookup = "java:jboss/datasources/BankingDS")
    private DataSource dataSource;

    @PostConstruct
    void migrate() {
        log.info("Running Flyway migrations against BankingDS...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load();
        var result = flyway.migrate();
        log.info("Flyway: " + result.migrationsExecuted + " migrations applied; current version="
                + result.targetSchemaVersion);
    }
}
