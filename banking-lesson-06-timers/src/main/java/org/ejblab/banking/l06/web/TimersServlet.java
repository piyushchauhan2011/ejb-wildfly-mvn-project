package org.ejblab.banking.l06.web;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ejblab.banking.l06.NightlyInterestBean;
import org.ejblab.banking.l06.ReminderScheduler;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

@WebServlet({"/run-interest", "/reminders"})
public class TimersServlet extends HttpServlet {

    @Inject NightlyInterestBean interest;
    @Inject ReminderScheduler reminders;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        var out = resp.getWriter();
        switch (req.getServletPath()) {
            case "/run-interest" -> {
                interest.runOnce(LocalDate.now());
                out.write("interest job triggered for " + LocalDate.now() + "\n");
            }
            case "/reminders" -> {
                String action = req.getParameter("action");
                switch (action == null ? "create" : action) {
                    case "create" -> {
                        long id = reminders.scheduleOnce(
                                req.getParameter("message"),
                                Duration.ofSeconds(Long.parseLong(
                                        req.getParameter("inSeconds") != null ?
                                                req.getParameter("inSeconds") : "60")));
                        out.write("created reminder " + id + "\n");
                    }
                    case "cancel" -> {
                        boolean ok = reminders.cancel(Long.parseLong(req.getParameter("id")));
                        out.write(ok ? "cancelled\n" : "not found\n");
                    }
                    case "list" -> reminders.listActive().forEach(id -> out.write("active: " + id + "\n"));
                    default -> resp.sendError(400, "unknown action");
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        var out = resp.getWriter();
        if (req.getServletPath().equals("/reminders")) {
            for (Long id : reminders.listActive()) out.write("active: " + id + "\n");
        }
    }
}
