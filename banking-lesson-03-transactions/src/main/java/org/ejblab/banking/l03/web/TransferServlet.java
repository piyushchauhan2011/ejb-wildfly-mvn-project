package org.ejblab.banking.l03.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.domain.InsufficientFundsException;
import org.ejblab.banking.l03.BmtTransferBean;
import org.ejblab.banking.l03.SeedBean;
import org.ejblab.banking.l03.SerializableRetryService;
import org.ejblab.banking.l03.TransferService;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Servlet to drive the lesson. Examples:
 * <pre>{@code
 * curl -X POST '.../seed'
 * curl -X POST '.../transfer?from=ACC-001&to=ACC-002&amount=10'
 * curl -X POST '.../transfer?mode=bmt&from=ACC-001&to=ACC-002&amount=10'
 * curl -X POST '.../transfer?mode=retry&from=ACC-001&to=ACC-002&amount=10'
 * }</pre>
 */
@WebServlet({"/transfer", "/seed"})
public class TransferServlet extends HttpServlet {

    @Inject TransferService cmt;
    @Inject BmtTransferBean bmt;
    @Inject SerializableRetryService retry;
    @Inject SeedBean seed;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        var out = resp.getWriter();

        if (req.getServletPath().equals("/seed")) {
            seed.seedIfMissing();
            seed.reset();
            out.write("seeded + reset: ACC-001 = 1000.00, ACC-002 = 1000.00\n");
            return;
        }

        String from = req.getParameter("from");
        String to = req.getParameter("to");
        BigDecimal amount = new BigDecimal(req.getParameter("amount"));
        String mode = req.getParameter("mode");

        try {
            switch (mode == null ? "cmt" : mode) {
                case "bmt" -> bmt.transfer(from, to, amount);
                case "retry" -> retry.transferWithRetry(from, to, amount);
                default -> cmt.transfer(from, to, amount);
            }
            out.write("OK: transferred " + amount + " from " + from + " to " + to + "\n");
        } catch (InsufficientFundsException ife) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.write("REJECTED: " + ife.getMessage() + "\n");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n");
        }
    }
}
