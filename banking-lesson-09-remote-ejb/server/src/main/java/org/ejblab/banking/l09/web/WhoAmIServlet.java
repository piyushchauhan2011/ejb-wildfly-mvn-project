package org.ejblab.banking.l09.web;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l09.api.AccountQuery;

import java.io.IOException;

/**
 * Sanity servlet on the SAME server that exposes the remote EJB. Lets you
 * confirm locally that the bean works before chasing remote-client issues.
 */
@WebServlet("/accounts")
public class WhoAmIServlet extends HttpServlet {
    @EJB AccountQuery q;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("accounts=" + q.listAccountNumbers());
    }
}
