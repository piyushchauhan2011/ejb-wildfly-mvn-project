package org.ejblab.banking.l10.web;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.Transfer;
import org.ejblab.banking.domain.TransferRequest;
import org.ejblab.banking.l10.TransferFacade;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

/**
 * All-in-one servlet serving a minimal banking UI. Three handlers:
 * <ul>
 *   <li>{@code GET /} — list accounts, show pending/completed transfers</li>
 *   <li>{@code POST /transfer} — submit a new TransferRequest</li>
 *   <li>{@code GET /transfer/{id}} — status poll</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"", "/", "/transfer", "/transfer/*"})
public class BankingServlet extends HttpServlet {

    @Inject TransferFacade facade;

    @PersistenceContext(unitName = "bankingPU") EntityManager em;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getServletPath().equals("/transfer") && req.getPathInfo() != null) {
            long id = Long.parseLong(req.getPathInfo().substring(1));
            Transfer t = em.find(Transfer.class, id);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("id=" + id + " status=" + (t == null ? "UNKNOWN" : t.getStatus()));
            return;
        }
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><title>Banking Lab</title>");
        out.println("<style>body{font-family:sans-serif;max-width:720px;margin:2rem auto;}"
                + "table{border-collapse:collapse;width:100%}th,td{padding:6px 10px;border:1px solid #ddd;text-align:left}"
                + "form{margin:1rem 0;padding:1rem;background:#f5f5f5;border-radius:6px}"
                + "input,button{padding:6px;margin-right:8px;font-size:14px}</style></head><body>");
        out.println("<h1>Banking Lab &mdash; End-to-End Flow</h1>");

        out.println("<h2>Accounts</h2><table><tr><th>#</th><th>Type</th><th>Balance</th></tr>");
        for (Account a : em.createQuery("from Account a order by a.accountNumber", Account.class).getResultList()) {
            out.println("<tr><td>" + a.getAccountNumber() + "</td><td>" + a.getType()
                    + "</td><td>" + a.getBalance() + "</td></tr>");
        }
        out.println("</table>");

        out.println("<h2>Transfer</h2><form method='post' action='transfer'>");
        out.println("<input name='from' placeholder='from' value='E2E-001'/>");
        out.println("<input name='to' placeholder='to' value='E2E-002'/>");
        out.println("<input name='amount' placeholder='amount' value='25.00'/>");
        out.println("<button type='submit'>Submit</button></form>");

        out.println("<h2>Recent transfers</h2><table><tr><th>id</th><th>client req</th><th>from</th><th>to</th><th>amount</th><th>status</th></tr>");
        for (Transfer t : em.createQuery("from Transfer t order by t.id desc", Transfer.class)
                .setMaxResults(20).getResultList()) {
            out.println("<tr><td>" + t.getId() + "</td><td>" + t.getClientRequestId()
                    + "</td><td>" + t.getFromAccount().getAccountNumber()
                    + "</td><td>" + t.getToAccount().getAccountNumber()
                    + "</td><td>" + t.getAmount() + "</td><td>" + t.getStatus() + "</td></tr>");
        }
        out.println("</table></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TransferRequest request = TransferRequest.of(
                req.getParameter("from"),
                req.getParameter("to"),
                new BigDecimal(req.getParameter("amount")));
        Transfer t = facade.submit(request);
        resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        resp.sendRedirect(req.getContextPath() + "/?accepted=" + t.getId());
    }
}
