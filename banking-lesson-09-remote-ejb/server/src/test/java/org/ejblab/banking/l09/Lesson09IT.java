package org.ejblab.banking.l09;

import javax.naming.InitialContext;
import org.ejblab.banking.l09.api.AccountQuery;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * In-container Arquillian sanity check for the remote EJB bean. A true
 * cross-JVM remote invocation is exercised by the client module's README
 * walkthrough; this test just makes sure the server war deploys and the
 * bean is reachable through CDI within the container.
 */
@ExtendWith(ArquillianExtension.class)
public class Lesson09IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "banking-lesson-09-remote-ejb-server.war")
                .addPackages(true, "org.ejblab.banking.l09", "org.ejblab.banking.l09.api",
                        "org.ejblab.banking.domain")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("db/migration/V1__baseline.sql", "db/migration/V1__baseline.sql")
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve("org.flywaydb:flyway-core", "org.flywaydb:flyway-database-postgresql")
                        .withTransitivity().asFile());
    }

    @Test
    void ping_returns_pong() throws Exception {
        AccountQuery query = (AccountQuery) new InitialContext().lookup(
                "java:global/banking-lesson-09-remote-ejb-server/AccountQueryBean!"
                        + AccountQuery.class.getName());
        assertNotNull(query);
        assertEquals("pong: hi", query.ping("hi"));
    }
}
