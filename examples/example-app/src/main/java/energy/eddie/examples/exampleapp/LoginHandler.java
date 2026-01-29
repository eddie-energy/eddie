// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.examples.exampleapp;

import com.google.inject.Inject;
import io.javalin.Javalin;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginHandler implements JavalinHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    private final Jdbi jdbi;

    @Inject
    public LoginHandler(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void register(Javalin app) {
        app.get("/login", ctx -> ctx.render("login-user.jte"));
        app.post("/login", ctx -> {
            var email = ctx.formParam("input-user");
            logger.info("Login with user: {}", email);
            if (null == email || email.isEmpty()) {
                ctx.redirect("login");
            } else {
                loginUser(email);
                ctx.sessionAttribute("user", email);
                ctx.redirect(ctx.contextPath() + "/connections/");
            }
        });
    }

    private void loginUser(String email) {
        var isUserFoundInDb = jdbi.withHandle(h ->
                h.createQuery("SELECT email FROM USERS WHERE email=?")
                        .bind(0, email)
                        .mapTo(String.class)
                        .findOne().isPresent()).booleanValue();
        if (!isUserFoundInDb) {
            logger.warn("User not found, creating user with email in db: {}", email);
            jdbi.withHandle(h -> h.execute("INSERT INTO USERS(email) VALUES (?)", email));
        }
    }
}
