package org.ejblab.banking.l08;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * Business entry-point. Method-level {@code @RolesAllowed} enforces who
 * can do what. The caller's identity propagates from the servlet (via
 * {@code jaspic} form login) into this EJB, so you don't have to lift
 * a finger to "carry the user".
 *
 * <p>Note the class-level {@link RunAs RunAs("SYSTEM")}: any downstream
 * bean the method calls sees principal {@code SYSTEM}, not the original
 * user. This is useful when {@code TransferFacade} needs to write an
 * audit event via {@link AuditService} (which requires AUDITOR/SYSTEM)
 * regardless of who the user is.
 */
@Stateless
@DeclareRoles({"TELLER", "CUSTOMER", "AUDITOR", "SYSTEM"})
@RunAs("SYSTEM")
public class TransferFacade {

    private static final Logger log = Logger.getLogger(TransferFacade.class.getName());

    @Inject AuditService audit;

    @Resource EJBContext ctx;

    @RolesAllowed({"TELLER", "CUSTOMER"})
    public String transfer(String from, String to, BigDecimal amount) {
        String user = ctx.getCallerPrincipal().getName();
        // NOTE: inside this method, ctx.getCallerPrincipal() is still the ORIGINAL
        // user, NOT the run-as principal. The run-as only kicks in when WE call
        // downstream EJBs (see below).
        log.info(user + " initiated transfer " + amount + " " + from + " -> " + to);

        audit.write("transfer requested by " + user);  // audit sees principal=SYSTEM (run-as)

        return "ok";
    }

    @RolesAllowed({"TELLER", "AUDITOR"})
    public String approve(String clientRequestId) {
        audit.write("approve by " + ctx.getCallerPrincipal().getName()
                + " for request " + clientRequestId);
        return "approved";
    }

    /** Anyone can peek at public terms & conditions. */
    @PermitAll
    public String terms() {
        return "Lesson 8 terms and conditions";
    }

    /** Explicit deny. Useful for decommissioned endpoints. */
    @jakarta.annotation.security.DenyAll
    public String deprecated() {
        throw new UnsupportedOperationException();
    }
}
