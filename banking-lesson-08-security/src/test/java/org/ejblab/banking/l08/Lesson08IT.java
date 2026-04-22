package org.ejblab.banking.l08;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ArquillianExtension.class)
public class Lesson08IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "banking-lesson-08-security.war")
                .addPackages(true, "org.ejblab.banking.l08", "org.ejblab.banking.domain")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("db/migration/V1__baseline.sql", "db/migration/V1__baseline.sql")
                .addAsResource("db/migration/V2__security.sql", "db/migration/V2__security.sql")
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve("org.flywaydb:flyway-core", "org.flywaydb:flyway-database-postgresql")
                        .withTransitivity().asFile());
    }

    @Test
    void termsIsPermitAll(TransferFacade facade) {
        assertTrue(facade.terms().contains("terms"));
    }
}
