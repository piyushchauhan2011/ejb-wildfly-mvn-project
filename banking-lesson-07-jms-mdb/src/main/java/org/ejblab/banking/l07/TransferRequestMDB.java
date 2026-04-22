package org.ejblab.banking.l07;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

import org.ejblab.banking.domain.TransferRequest;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-Driven Bean: consumes from {@code TransfersRequested} and invokes
 * the synchronous {@link TransferService} to do the actual DB work.
 *
 * <p>Why split MDB vs TransferService?
 * <ul>
 *   <li>{@code TransferService} is called in both the sync path (direct
 *       servlet) and the async path (via this MDB) - two delivery mechanisms,
 *       one business logic.</li>
 *   <li>This MDB is thin, focused on message -> invocation translation.</li>
 * </ul>
 *
 * <p>Transactional guarantees: because we use the XA connection factory
 * (messaging-activemq's default) AND an XA datasource, JMS consumption +
 * JDBC writes are committed together via two-phase commit. A crash
 * mid-flight either "consumed + persisted" or "neither".
 *
 * <p>Redelivery: configured in the JMS CLI script to 3 attempts with
 * exponential backoff, then routed to {@code TransfersDLQ}.
 */
@MessageDriven(name = "TransferRequestMDB",
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = "java:/jms/queue/TransfersRequested"),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "5")
    })
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TransferRequestMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(TransferRequestMDB.class.getName());

    @Inject TransferService transferService;

    @Resource MessageDrivenContext mdc;

    @Override
    public void onMessage(Message message) {
        try {
            TransferRequest req = ((ObjectMessage) message).getBody(TransferRequest.class);
            int redeliveries = message.getIntProperty("JMSXDeliveryCount") - 1;
            if (redeliveries > 0) {
                log.log(Level.WARNING, "redelivery #{0} for clientRequestId={1}",
                        new Object[]{redeliveries, req.clientRequestId()});
            }
            transferService.transfer(req);  // REQUIRED joins the MDB's TX
        } catch (JMSException e) {
            // JMS plumbing error - not business. Force rollback so redelivery kicks in.
            log.log(Level.SEVERE, "bad JMS message", e);
            mdc.setRollbackOnly();
        } catch (RuntimeException e) {
            // Business failure - rollback so redelivery can try again;
            // after max-delivery-attempts, the message is routed to TransfersDLQ.
            log.log(Level.WARNING, "transfer failed, will be redelivered", e);
            mdc.setRollbackOnly();
            throw e;   // also report to the broker's listener for metrics
        }
    }
}
