package org.ejblab.banking.l01.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet({"", "/"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write("""
                <!doctype html><html><head><title>Lesson 1 - EJB Refresher</title></head>
                <body style="font-family: system-ui, sans-serif; max-width: 720px; margin: 2rem auto;">
                  <h1>Banking Lab - Lesson 1: EJB Refresher</h1>
                  <ul>
                    <li><a href="./greet?name=Ada">GET /greet?name=Ada</a> - @Stateless bean</li>
                    <li><a href="./accounts">GET /accounts</a> - @Singleton size</li>
                    <li><a href="./accounts?number=GB29NWBK60161331926819">GET /accounts?number=...</a></li>
                    <li><a href="./cart">GET /cart</a>  -  @Stateful session (POST to add)</li>
                  </ul>
                  <p>Read <code>README.md</code> for the guided walkthrough and interview Q&amp;A.</p>
                </body></html>
                """);
    }
}
