package org.ejblab.banking.l01.web;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.ejblab.banking.l01.CartBean;

import java.io.IOException;

/**
 * Demonstrates Stateful bean lifecycle.
 *
 * <p>The Stateful instance is <strong>not</strong> automatically tied to the
 * servlet session - we manually bind the proxy to the {@link HttpSession} so
 * every request from the same browser session uses the same Stateful instance.
 *
 * <p>Why do we inject a proxy and cache it in the session?
 * <br>The {@code @Inject CartBean} yields a fresh instance every injection
 * point in a Stateful bean. To preserve state across requests we must stash
 * the proxy ourselves.
 *
 * <p>Try:
 * <pre>{@code
 * curl -c cookies.txt -b cookies.txt -X POST http://.../cart?sku=ABC
 * curl -c cookies.txt -b cookies.txt -X POST http://.../cart?sku=DEF
 * curl -c cookies.txt -b cookies.txt http://.../cart
 * curl -c cookies.txt -b cookies.txt -X POST http://.../cart?action=checkout
 * }</pre>
 */
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private static final String CART_ATTR = "l01.cart";

    @Inject
    private jakarta.enterprise.inject.Instance<CartBean> cartFactory;

    private CartBean cartFor(HttpSession session) {
        CartBean cart = (CartBean) session.getAttribute(CART_ATTR);
        if (cart == null) {
            cart = cartFactory.get();
            session.setAttribute(CART_ATTR, cart);
        }
        return cart;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CartBean cart = cartFor(req.getSession());
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("cart items (" + cart.size() + "): " + cart.items());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        CartBean cart = cartFor(session);
        String action = req.getParameter("action");

        resp.setContentType("text/plain;charset=UTF-8");
        if ("checkout".equalsIgnoreCase(action)) {
            var order = cart.checkout();
            session.removeAttribute(CART_ATTR);
            resp.getWriter().write("checked out " + order.size() + " items: " + order);
            return;
        }

        String sku = req.getParameter("sku");
        if (sku == null || sku.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "sku is required");
            return;
        }
        cart.add(sku);
        resp.getWriter().write("added " + sku + "; cart now has " + cart.size());
    }
}
