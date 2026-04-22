package org.ejblab.banking.l08;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@Startup
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class MigrationsBean {

    @Resource(lookup = "java:jboss/datasources/BankingDS")
    private DataSource dataSource;

    @PostConstruct
    void migrate() {
        Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).locations("classpath:db/migration")
                .load().migrate();
    }
}
