package energy.eddie.epdemoapp;

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

    @Inject
    private Jdbi jdbi;

    @Override
    public void register(Javalin app) {
        app.get("/connections/", this::listConnections);
        jdbi.withHandle(h -> h.execute("""
                CREATE TABLE IF NOT EXISTS CONNECTIONS (
                    user_id INTEGER NOT NULL,
                    connection_id VARCHAR2(255) NOT NULL PRIMARY KEY
                );
                CREATE SEQUENCE IF NOT EXISTS CONNECTION_ID_SEQ AS BIGINT;
                """));
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
        logger.info("next connection id for user {} is {}", user, nextConnectionId);
        var connectionStatuses = jdbi.withHandle(h ->
                h.createQuery("""
                                select CONNECTION_ID, TIMESTAMP_, CONSENT_STATUS
                                from CONNECTION_STATUS as out
                                where TIMESTAMP_ = (select max(TIMESTAMP_) from CONNECTION_STATUS as inn where out.CONNECTION_ID = inn.CONNECTION_ID)
                                    and CONNECTION_ID in (select CONNECTION_ID from CONNECTIONS
                                        where USER_ID=(select ID from USERS where EMAIL=?))
                                order by CONNECTION_ID
                                """)
                        .bind(0, user)
                        .map((rs, ct) -> new ConnectionStatus(rs.getString(1), rs.getObject(2), rs.getString(3)))
                        .list());
        Map<String, Object> x = Map.of("user", user,
                "connectionStatuses", connectionStatuses,
                "nextConnectionId", nextConnectionId);
        ctx.render("connections.jte", x);
    }
}
