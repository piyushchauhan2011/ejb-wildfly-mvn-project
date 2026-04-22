package org.ejblab.banking.l07;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Runs Flyway migrations at deploy time.
 *
 * <p>This is a teaching lab where every lesson shares the same Postgres
 * database. To keep each lesson's V1 migration independent of the others
 * we drop everything in the schema before re-applying migrations.
 * <strong>Never</strong> do this in production - use ordered Vn migrations,
 * no clean().
 */
@Startup
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class MigrationsBean {

    @Resource(lookup = "java:jboss/datasources/BankingXADS")
    private DataSource dataSource;

    @PostConstruct
    void migrate() {
        Flyway fw = Flyway.configure()
                .dataSource(dataSource)
                .cleanDisabled(false)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load();
        fw.clean();
        fw.migrate();
    }
}
