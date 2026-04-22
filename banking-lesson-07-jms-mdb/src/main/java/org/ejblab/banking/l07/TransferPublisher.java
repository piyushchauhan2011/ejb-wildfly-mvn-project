package org.ejblab.banking.l07;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;

import org.ejblab.banking.domain.TransferRequest;

/**
 * Sends a {@link TransferRequest} onto the "TransfersRequested" queue.
 *
 * <p>Using {@link JMSContext} (the Jakarta EE 10 convenience API) is shorter
 * and - when you don't annotate with {@code @Resource} a specific XA
 * factory - defaults to the default XA-enabled in-VM connection factory.
 * That's what makes the servlet-side send enlist in the caller's TX,
 * so the "enqueue + DB write" combo is atomic.
 */
@Stateless
public class TransferPublisher {

    @Resource(lookup = "java:comp/DefaultJMSConnectionFactory")
    ConnectionFactory cf;

    @Resource(lookup = "java:/jms/queue/TransfersRequested")
    Queue queue;

    public void publish(TransferRequest req) {
        try (JMSContext jms = cf.createContext()) {
            JMSProducer producer = jms.createProducer();
            producer.setProperty("clientRequestId", req.clientRequestId());
            producer.send(queue, req);
        }
    }
}
