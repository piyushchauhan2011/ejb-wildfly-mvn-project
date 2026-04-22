package org.ejblab.banking.l12.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l12.MeteredCalculator;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

@WebServlet({"/", "/calc", "/help"})
public class ObsServlet extends HttpServlet {

    @Inject MeteredCalculator calc;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        switch (req.getServletPath()) {
            case "/calc" -> {
                BigDecimal balance = new BigDecimal(req.getParameter("balance"));
                BigDecimal rate = new BigDecimal(req.getParameter("rate"));
                int days = Integer.parseInt(req.getParameter("days"));
                out.print("accrued=" + calc.accrue(balance, rate, days));
            }
            default -> {
                out.println("Lesson 12 - Observability");
                out.println();
                out.println("Try:");
                out.println("  GET  /banking-lesson-12-observability/calc?balance=1000&rate=0.05&days=30");
                out.println();
                out.println("Management endpoints (on http://localhost:9990):");
                out.println("  GET  /health                (aggregate)");
                out.println("  GET  /health/live           (JVM salvageable?)");
                out.println("  GET  /health/ready          (DB reachable?)");
                out.println("  GET  /metrics               (OpenMetrics text)");
                out.println();
                out.println("Useful CLI queries:");
                out.println("  /subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:read-resource");
                out.println("  /subsystem=datasources/data-source=BankingDS/statistics=pool:read-resource(include-runtime=true)");
            }
        }
    }
}
