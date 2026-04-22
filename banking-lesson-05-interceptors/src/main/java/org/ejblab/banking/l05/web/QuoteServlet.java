package org.ejblab.banking.l05.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l05.AuditTrail;
import org.ejblab.banking.l05.QuoteService;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet({"/quote", "/quote/fx", "/quote/broken", "/audit"})
public class QuoteServlet extends HttpServlet {

    @Inject QuoteService quotes;
    @Inject AuditTrail trail;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        var out = resp.getWriter();
        switch (req.getServletPath()) {
            case "/quote" -> out.write("fee = " + quotes.quoteTransferFee(
                    new BigDecimal(req.getParameter("amount"))));
            case "/quote/fx" -> out.write("rate = " + quotes.quoteFxRate(
                    req.getParameter("from"), req.getParameter("to")));
            case "/quote/broken" -> {
                try { quotes.quoteBroken(); }
                catch (Exception e) { out.write("caught " + e.getMessage()); }
            }
            case "/audit" -> {
                for (AuditTrail.Entry e : trail.all()) {
                    out.write(e.at() + " " + e.target() + " " + e.args()
                            + " -> " + e.outcome() + " " + e.durationMicros() + "us"
                            + (e.error() != null ? " [" + e.error() + "]" : "") + "\n");
                }
            }
        }
    }
}
