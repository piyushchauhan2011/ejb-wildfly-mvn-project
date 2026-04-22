package org.ejblab.banking.l09.client;

import org.ejblab.banking.l09.api.AccountQuery;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Hashtable;

/**
 * Standalone Java client that looks up the remote EJB over HTTP-remoting.
 *
 * <p>Two valid configuration styles are demonstrated:
 * <ol>
 *   <li>Programmatic JNDI via {@code InitialContext} + {@code jboss-ejb-client}
 *       properties (shown here).</li>
 *   <li>File-based: drop {@code wildfly-config.xml} on the classpath — see
 *       {@code src/main/resources/wildfly-config.xml}. With that file present
 *       no programmatic properties are needed.</li>
 * </ol>
 *
 * <p>Run:
 * <pre>
 * ./mvnw -pl banking-lesson-09-remote-ejb/client package
 * java -jar banking-lesson-09-remote-ejb/client/target/*.jar
 * </pre>
 */
public class RemoteClient {

    public static void main(String[] args) throws NamingException {
        String host = System.getProperty("host", "localhost");
        String port = System.getProperty("port", "8080");
        String user = System.getProperty("user", "alice");
        String pass = System.getProperty("pass", "alice123");

        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        env.put(Context.PROVIDER_URL, "remote+http://" + host + ":" + port);
        env.put(Context.SECURITY_PRINCIPAL, user);
        env.put(Context.SECURITY_CREDENTIALS, pass);
        env.put("jboss.naming.client.ejb.context", true);

        InitialContext ctx = new InitialContext(env);
        try {
            String jndi = "ejb:/banking-lesson-09-remote-ejb-server/AccountQueryBean!"
                    + AccountQuery.class.getName();
            AccountQuery q = (AccountQuery) ctx.lookup(jndi);

            System.out.println("ping -> " + q.ping("hi"));
            System.out.println("accounts -> " + q.listAccountNumbers());
            System.out.println("balance REMOTE-001 -> " + q.balanceOf("REMOTE-001"));
        } finally {
            ctx.close();
        }
    }
}
