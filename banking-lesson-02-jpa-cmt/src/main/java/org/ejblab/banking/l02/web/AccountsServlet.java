package org.ejblab.banking.l02.web;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.AccountType;
import org.ejblab.banking.domain.Customer;
import org.ejblab.banking.l02.AccountRepository;
import org.ejblab.banking.l02.CustomerRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * CRUD-ish servlet over the JPA repositories.
 * <ul>
 *   <li>GET  /accounts          -> list (count + rows)</li>
 *   <li>POST /accounts?owner=&amp;balance=1000 -> create a demo account</li>
 * </ul>
 */
@WebServlet("/accounts")
public class AccountsServlet extends HttpServlet {

    @Inject
    AccountRepository accountRepo;

    @Inject
    CustomerRepository customerRepo;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        var w = resp.getWriter();
        long count = accountRepo.count();
        w.write("accounts: " + count + "\n");
        for (Account a : accountRepo.findByCustomer(
                parseLongOrDefault(req.getParameter("customerId"), 0L))) {
            w.write("  " + a.getAccountNumber()
                    + " | " + a.getType()
                    + " | " + a.getBalance()
                    + " | v" + a.getVersion() + "\n");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ownerName = req.getParameter("owner");
        String balanceStr = req.getParameter("balance");
        String email = req.getParameter("email");

        if (ownerName == null || email == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "owner and email are required");
            return;
        }
        BigDecimal initial = balanceStr == null ? BigDecimal.ZERO : new BigDecimal(balanceStr);

        Customer owner = customerRepo.findByEmail(email)
                .orElseGet(() -> customerRepo.save(new Customer(ownerName, email)));
        Account a = new Account(
                "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                owner, AccountType.CHECKING, initial);
        a = accountRepo.save(a);

        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("created account " + a.getAccountNumber()
                + " for customer " + owner.getId());
    }

    private static long parseLongOrDefault(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return def; }
    }
}
