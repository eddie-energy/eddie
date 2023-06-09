package energy.eddie.examples.exampleapp;

import com.google.inject.Inject;
import io.javalin.Javalin;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginHandler implements JavalinHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    @Inject
    private Jdbi jdbi;

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
        final var createUserTableSql = """                
                CREATE TABLE IF NOT EXISTS USERS (
                    id SERIAL PRIMARY KEY,
                    email VARCHAR(80)
                );
                CREATE INDEX IF NOT EXISTS INDEX_USER_EMAIL ON USERS (email);
                """;
        jdbi.withHandle(h -> h.execute(createUserTableSql));
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
