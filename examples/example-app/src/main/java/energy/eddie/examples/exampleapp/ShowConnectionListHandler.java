package energy.eddie.examples.exampleapp;

import com.google.inject.Inject;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.annotation.Nullable;

import java.util.Map;

public class ShowConnectionListHandler implements JavalinHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShowConnectionListHandler.class);

    private final Jdbi jdbi;

    @Inject
    public ShowConnectionListHandler(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void register(Javalin app) {
        app.get("/connections/", this::listConnections);
        final var createTableConnectionsSql = """
                CREATE TABLE IF NOT EXISTS CONNECTIONS (
                    user_id INTEGER NOT NULL,
                    connection_id VARCHAR(255) NOT NULL PRIMARY KEY
                );
                CREATE SEQUENCE IF NOT EXISTS CONNECTION_ID_SEQ AS BIGINT;
                """;
        jdbi.withHandle(h -> h.execute(createTableConnectionsSql));
    }

    @Nullable
    private String getNextIdFor(String user) {
        var id = jdbi.withHandle(h -> h.createQuery("""
                        SELECT connection_id FROM CONNECTIONS AS c WHERE
                         user_id=(SELECT id FROM USERS WHERE email=?) AND NOT EXISTS(
                            SELECT * FROM CONNECTION_STATUS AS cs WHERE c.connection_id=cs.connection_id)
                        """)
                .bind(0, user)
                .mapTo(String.class)
                .findFirst());
        if (id.isEmpty()) {
            id = jdbi.withHandle(h -> h.createUpdate("""
                            INSERT INTO CONNECTIONS(user_id, connection_id) VALUES (
                                (SELECT id FROM USERS WHERE email=?), nextval('CONNECTION_ID_SEQ'))
                            """)
                    .bind(0, user)
                    .executeAndReturnGeneratedKeys("connection_id")
                    .mapTo(String.class)
                    .findFirst());
        }

        return id.orElse(null);
    }

    private void listConnections(Context ctx) {
        String user = ctx.sessionAttribute("user");
        String nextConnectionId = getNextIdFor(user);
        logger.info("Next connection id for user {} is {}", user, nextConnectionId);
        final var selectConnectionStatusesSql = """
                SELECT connection_id, timestamp_, consent_status, row_number
                FROM connection_status AS out
                WHERE row_number = (SELECT MAX(row_number) FROM connection_status AS inn WHERE out.connection_id = inn.connection_id)
                    AND connection_id IN (SELECT connection_id FROM connections
                        WHERE user_id=(SELECT id FROM users WHERE email=?))
                ORDER BY connection_id
                """;
        var connectionStatuses = jdbi.withHandle(h ->
                h.createQuery(selectConnectionStatusesSql)
                        .bind(0, user)
                        .map((rs, ct) -> new ConnectionStatus(rs.getString(1), rs.getObject(2), rs.getString(3)))
                        .list());
        Map<String, Object> x = Map.of("user", user,
                "connectionStatuses", connectionStatuses,
                "nextConnectionId", nextConnectionId);
        ctx.render("connections.jte", x);
    }
}
