package energy.eddie.framework;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;

public class JdbcAdapter implements energy.eddie.api.v0.ApplicationConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAdapter.class);

    private final Jdbi jdbi;

    public JdbcAdapter(String jdbcUrl, String userName, String password) {
        if (null != userName && null != password) {
            jdbi = Jdbi.create(jdbcUrl, userName, password);
        } else {
            jdbi = Jdbi.create(jdbcUrl);
        }
    }

    @Override
    public void setConnectionStatusMessageStream(Flow.Publisher<ConnectionStatusMessage> csmsFlow) {
        var csms = JdkFlowAdapter.flowPublisherToFlux(csmsFlow);
        csms.subscribe(csm -> {
            LOGGER.info("writing connection status for {}: {}", csm.connectionId(), csm.status());
            jdbi.withHandle(h ->
                    h.createUpdate("insert into connection_status (connection_id, timestamp_, consent_status) values (?,?,?)")
                            .bind(0, csm.connectionId())
                            .bind(1, csm.timestamp())
                            .bind(2, csm.status())
                            .execute());
        });
    }

    @Override
    public void setConsumptionRecordStream(Flow.Publisher<ConsumptionRecord> crsFlow) {
        LOGGER.error("calling unimplemented interface method JdbcAdapter.setConsumptionRecordStream(..)");
        var crs = JdkFlowAdapter.flowPublisherToFlux(crsFlow);
        crs.subscribe(cr -> {
            LOGGER.info("writing consumption records for connection {} with metering point {} with {} data points",
                    cr.getConnectionId(), cr.getMeteringPoint(), cr.getConsumptionPoints().size());
            jdbi.withHandle(h -> {
                var id = h.createUpdate("""
                                INSERT INTO CONSUMPTION_RECORDS(connection_id, metering_point,start_date_time,metering_interval_secs) VALUES (?,?,?,?)
                                """)
                        .bind(0, cr.getConnectionId())
                        .bind(1, cr.getMeteringPoint())
                        .bind(2, cr.getStartDateTime())
                        .bind(3, meteringIntervalForCode.get(cr.getMeteringInterval().value()))
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Integer.class)
                        .first();
                var cps = cr.getConsumptionPoints();
                for (int i = 0; i < cps.size(); ++i) {
                    h.createUpdate("""
                                    INSERT INTO CONSUMPTION_POINTS(consumption_record_id, ord, consumption, metering_type) VALUES (?, ?, ?, ?)
                                    """)
                            .bind(0, id)
                            .bind(1, i)
                            .bind(2, cps.get(i).getConsumption())
                            .bind(3, cps.get(i).getMeteringType())
                            .execute();
                }
                return null;
            });
        });
    }

    @Override
    public void init() {
        LOGGER.info("initializing db schema");
        jdbi.withHandle(h -> {
            h.execute("""
                    CREATE TABLE IF NOT EXISTS CONNECTION_STATUS (
                        connection_id  VARCHAR(255) NOT NULL,
                        timestamp_     TIMESTAMP WITH TIME ZONE  NOT NULL,
                        consent_status VARCHAR(32) NOT NULL
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
                    """);
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

    private static final Map<String, Integer> meteringIntervalForCode;

    static {
        meteringIntervalForCode = new HashMap<>();
        meteringIntervalForCode.put("PT15M", 900);
        meteringIntervalForCode.put("PT30M", 1800);
        meteringIntervalForCode.put("PT1H", 3600);
        meteringIntervalForCode.put("PT1D", 86400);
    }
}
