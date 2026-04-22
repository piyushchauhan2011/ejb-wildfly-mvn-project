package org.ejblab.banking.l10;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

import org.ejblab.banking.domain.TransferRequest;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MDB bridging {@link TransferFacade} (stage 1) and {@link TransferProcessor}
 * (stage 2). Stays short and dumb: deserialize, delegate.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/TransfersRequested"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class TransferRequestMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(TransferRequestMDB.class.getName());

    @Inject TransferProcessor processor;

    @Override
    public void onMessage(Message message) {
        try {
            TransferRequest req = message.getBody(TransferRequest.class);
            if (req == null && message instanceof ObjectMessage om) {
                req = (TransferRequest) om.getObject();
            }
            if (req == null) {
                log.warning("MDB got null payload; skipping");
                return;
            }
            processor.process(req);
        } catch (JMSException e) {
            log.log(Level.SEVERE, "JMS failure in MDB; message will be redelivered/DLQ-ed", e);
            throw new RuntimeException(e);
        }
    }
}
