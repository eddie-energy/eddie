package energy.eddie.examples.exampleapp;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JdbcAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAdapter.class);
    private static final Map<String, Integer> meteringIntervalForCode = Map.of(
            "PT15M", 900,
            "PT30M", 1800,
            "PT1H", 3600,
            "PT1D", 86400,
            "P1D", 86400
    );


    private final Jdbi jdbi;

    @Inject
    public JdbcAdapter(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void initializeDatabase() {
        LOGGER.info("Initializing db schema");
        final var createTablesSql = """
                CREATE TABLE IF NOT EXISTS CONNECTION_STATUS (
                    connection_id  VARCHAR(255) NOT NULL,
                    timestamp_     TIMESTAMP WITH TIME ZONE  NOT NULL,
                    consent_status VARCHAR(48) NOT NULL,
                    row_number SERIAL NOT NULL
                );

                CREATE TABLE IF NOT EXISTS METERING_INTERVALS (
                    metering_interval_secs INTEGER NOT NULL PRIMARY KEY,
                    code VARCHAR(16) NOT NULL
                );

                CREATE TABLE IF NOT EXISTS CONSUMPTION_RECORDS (
                    id                     SERIAL PRIMARY KEY,
                    connection_id          VARCHAR(255) NOT NULL,
                    metering_point         VARCHAR(255),
                    start_date_time        TIMESTAMP WITH TIME ZONE NOT NULL,
                    metering_interval_secs INTEGER NOT NULL,
                    FOREIGN KEY (metering_interval_secs) REFERENCES METERING_INTERVALS(metering_interval_secs)
                );

                CREATE TABLE IF NOT EXISTS CONSUMPTION_POINTS (
                    consumption_record_id INTEGER NOT NULL,
                    ord                   INTEGER NOT NULL,
                    consumption           DOUBLE PRECISION NOT NULL,
                    metering_type         VARCHAR(32) NOT NULL,
                    PRIMARY KEY (consumption_record_id, ord),
                    FOREIGN KEY (consumption_record_id) REFERENCES CONSUMPTION_RECORDS(id)
                );
                """;
        jdbi.withHandle(h -> {
            h.execute(createTablesSql);
            meteringIntervalForCode.forEach((key, value) -> {
                var count = h.createQuery("SELECT COUNT(*) FROM METERING_INTERVALS WHERE metering_interval_secs=?")
                        .bind(0, value)
                        .mapTo(Integer.class)
                        .first();
                if (null != count && count == 0) {
                    h.createUpdate("INSERT INTO METERING_INTERVALS (metering_interval_secs, code) VALUES (?,?)")
                            .bind(0, value)
                            .bind(1, key)
                            .execute();
                }
            });
            return null;
        });
    }
}
