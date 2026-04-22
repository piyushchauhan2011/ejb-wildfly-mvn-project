package org.ejblab.banking.l08.web;

import jakarta.annotation.security.DeclareRoles;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l08.TransferFacade;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Servlet-level authentication + authorization.
 *
 * <p>{@link ServletSecurity} + {@link HttpConstraint} restrict access to
 * authenticated users in any of {@code TELLER}, {@code CUSTOMER},
 * {@code AUDITOR}. Pair with form-login in {@code web.xml} to get a
 * friendly sign-in page.
 */
@WebServlet({"/transfer", "/terms", "/who", "/logout"})
@ServletSecurity(@HttpConstraint(rolesAllowed = {"TELLER", "CUSTOMER", "AUDITOR"}))
@DeclareRoles({"TELLER", "CUSTOMER", "AUDITOR"})
public class TransferServlet extends HttpServlet {

    @Inject TransferFacade facade;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        switch (req.getServletPath()) {
            case "/terms" -> resp.getWriter().write(facade.terms());
            case "/who" -> resp.getWriter().write(
                    "user=" + (req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "(none)")
                    + " TELLER?=" + req.isUserInRole("TELLER")
                    + " CUSTOMER?=" + req.isUserInRole("CUSTOMER")
                    + " AUDITOR?=" + req.isUserInRole("AUDITOR"));
            case "/logout" -> { req.logout(); resp.getWriter().write("bye"); }
            default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!req.getServletPath().equals("/transfer")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("text/plain;charset=UTF-8");
        try {
            String result = facade.transfer(
                    req.getParameter("from"),
                    req.getParameter("to"),
                    new BigDecimal(req.getParameter("amount")));
            resp.getWriter().write(result);
        } catch (jakarta.ejb.EJBAccessException e) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("forbidden");
        }
    }
}
