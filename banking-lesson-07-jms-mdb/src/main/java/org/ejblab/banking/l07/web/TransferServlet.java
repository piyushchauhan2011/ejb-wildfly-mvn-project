package org.ejblab.banking.l07.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.domain.TransferRequest;
import org.ejblab.banking.l07.SeedBean;
import org.ejblab.banking.l07.TransferPublisher;
import org.ejblab.banking.l07.TransferService;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * <pre>{@code
 *   curl -X POST '/seed'
 *   curl -X POST '/transfer?mode=sync  &from=L7-001&to=L7-002&amount=1'
 *   curl -X POST '/transfer?mode=async &from=L7-001&to=L7-002&amount=1'
 * }</pre>
 */
@WebServlet({"/transfer", "/seed"})
public class TransferServlet extends HttpServlet {

    @Inject TransferService sync;
    @Inject TransferPublisher publisher;
    @Inject SeedBean seed;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        if (req.getServletPath().equals("/seed")) {
            seed.seedIfMissing();
            resp.getWriter().write("seeded L7-001, L7-002 with 1000.00 each\n");
            return;
        }

        String mode = req.getParameter("mode");
        TransferRequest tr = TransferRequest.of(
                req.getParameter("from"),
                req.getParameter("to"),
                new BigDecimal(req.getParameter("amount")));

        long t0 = System.nanoTime();
        if ("async".equals(mode)) {
            publisher.publish(tr);
            resp.getWriter().write("enqueued " + tr.clientRequestId()
                    + " in " + (System.nanoTime() - t0) / 1_000 + " us\n");
        } else {
            var t = sync.transfer(tr);
            resp.getWriter().write("completed " + t.getClientRequestId()
                    + " in " + (System.nanoTime() - t0) / 1_000 + " us\n");
        }
    }
}
