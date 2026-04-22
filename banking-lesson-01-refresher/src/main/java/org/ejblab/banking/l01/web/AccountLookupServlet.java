package org.ejblab.banking.l01.web;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l01.AccountCatalogSingleton;

import java.io.IOException;

/**
 * Servlet that queries the {@code @Singleton} catalog.
 *
 * <p>Observe: here we use CDI {@code @Inject}. In modern Jakarta EE,
 * {@code @Inject} is preferred; {@code @EJB} is still required in
 * pre-CDI contexts (for example when injecting into a non-CDI-managed
 * class such as a plain JAX-RS filter in a legacy app).
 *
 * <p>Try: {@code curl 'http://localhost:8080/banking-lesson-01-refresher/accounts?number=GB29NWBK60161331926819'}
 */
@WebServlet("/accounts")
public class AccountLookupServlet extends HttpServlet {

    @Inject
    private AccountCatalogSingleton catalog;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String number = req.getParameter("number");
        resp.setContentType("text/plain;charset=UTF-8");
        if (number == null) {
            resp.getWriter().write("catalog size = " + catalog.size());
            return;
        }
        resp.getWriter().write(
                catalog.ownerOf(number)
                       .map(owner -> number + " -> " + owner)
                       .orElse(number + " -> (not found)"));
    }
}
