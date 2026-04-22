package org.ejblab.banking.l11.web;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.l11.AdjustmentService;
import org.ejblab.banking.l11.BasketBean;
import org.ejblab.banking.l11.TransferCommand;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Thin demo UI for lesson 11.
 *
 * <ul>
 *   <li>{@code GET /basket/add?sku=...} — add to a stateful basket (HttpSession-scoped)</li>
 *   <li>{@code GET /basket/checkout} — @Remove the stateful bean</li>
 *   <li>{@code POST /adjust} — validated + optimistically-retrying balance change</li>
 * </ul>
 */
@WebServlet({"/basket/add", "/basket/checkout", "/basket", "/adjust", "/accounts"})
public class PatternsServlet extends HttpServlet {

    @Inject BasketBean basket;   // injected via CDI — HttpSession-scoped proxy

    @Inject AdjustmentService adjuster;

    @PersistenceContext(unitName = "bankingPU") EntityManager em;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        switch (req.getServletPath()) {
            case "/basket/add" -> {
                basket.add(req.getParameter("sku"));
                resp.getWriter().write("basket=" + basket.items());
            }
            case "/basket/checkout" -> {
                basket.checkout();
                resp.getWriter().write("checked out");
            }
            case "/basket" -> resp.getWriter().write("basket=" + basket.items());
            case "/accounts" -> {
                StringBuilder out = new StringBuilder();
                for (Account a : em.createQuery("from Account a order by a.accountNumber", Account.class).getResultList()) {
                    out.append(a.getAccountNumber()).append(" -> ")
                       .append(a.getBalance()).append(" (v=")
                       .append(a.getVersion()).append(")\n");
                }
                resp.getWriter().write(out.toString());
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!req.getServletPath().equals("/adjust")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            TransferCommand cmd = new TransferCommand(
                    req.getParameter("from"),
                    req.getParameter("to"),
                    new BigDecimal(req.getParameter("amount")));
            BigDecimal newBalance = adjuster.adjust(cmd.fromAccount(), cmd);
            resp.getWriter().write("new balance=" + newBalance);
        } catch (jakarta.validation.ConstraintViolationException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("validation failed: " + e.getConstraintViolations());
        } catch (jakarta.ejb.EJBException | jakarta.persistence.OptimisticLockException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("conflict after retries: " + e.getMessage());
        }
    }
}
