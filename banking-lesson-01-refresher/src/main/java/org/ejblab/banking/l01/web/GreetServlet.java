package org.ejblab.banking.l01.web;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l01.GreetingBean;

import java.io.IOException;

/**
 * Plain Jakarta Servlet that invokes an {@code @Stateless} EJB.
 *
 * <p>{@code @EJB} is the EJB-spec injection annotation. In a web module the
 * container resolves the bean by type (or by mappedName / lookup for ambiguity).
 * You could equally use CDI {@code @Inject} and it would work; for a refresher
 * I show {@code @EJB} on purpose because many interview questions come from
 * legacy apps still using it.
 *
 * <p>Try: {@code curl 'http://localhost:8080/banking-lesson-01-refresher/greet?name=Ada'}
 */
@WebServlet("/greet")
public class GreetServlet extends HttpServlet {

    @EJB
    private GreetingBean greetingBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write(greetingBean.hello(req.getParameter("name")));
    }
}
