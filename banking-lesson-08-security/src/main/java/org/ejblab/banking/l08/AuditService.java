package org.ejblab.banking.l08;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.annotation.Resource;

import java.util.logging.Logger;

/**
 * Restricted helper that writes audit records. Only {@code AUDITOR} may
 * call it directly, BUT we also allow a {@link RunAs}-scoped bean to
 * act as AUDITOR internally (see {@link TransferFacade}).
 *
 * <p>This is the canonical "I'm a system action on behalf of a user"
 * pattern. Without {@code @RunAs}, calls from non-AUDITOR callers would
 * throw {@link jakarta.ejb.EJBAccessException}.
 */
@Stateless
@DeclareRoles({"AUDITOR", "TELLER", "CUSTOMER", "SYSTEM"})
@RolesAllowed({"AUDITOR", "SYSTEM"})
public class AuditService {

    private static final Logger log = Logger.getLogger("AUDIT");

    @Resource
    EJBContext ctx;

    public void write(String event) {
        String who = ctx.getCallerPrincipal().getName();
        log.info("AUDIT by " + who + ": " + event);
    }
}
