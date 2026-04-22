package org.ejblab.banking.l10;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

import org.ejblab.banking.domain.TransferRequest;

import java.util.logging.Logger;

/**
 * Stage 3: fan-out notifier. In a real system this would send email/SMS/push.
 * Here we simply log, to keep the lesson focused on the flow.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/TransfersCompleted"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class TransferNotifierMDB implements MessageListener {

    private static final Logger log = Logger.getLogger("NOTIFY");

    @Override
    public void onMessage(Message message) {
        try {
            TransferRequest req = message.getBody(TransferRequest.class);
            if (req == null && message instanceof ObjectMessage om) {
                req = (TransferRequest) om.getObject();
            }
            if (req != null) {
                log.info("[EMAIL STUB] transfer " + req.clientRequestId() + " completed: "
                        + req.fromAccountNumber() + " -> " + req.toAccountNumber()
                        + " " + req.amount());
            }
        } catch (Exception e) {
            log.warning("notifier failed (not rethrown, notifications are best-effort): " + e);
        }
    }
}
