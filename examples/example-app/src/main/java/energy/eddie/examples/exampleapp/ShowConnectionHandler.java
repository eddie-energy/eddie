package energy.eddie.examples.exampleapp;

import com.google.inject.Inject;
import io.javalin.Javalin;
import org.jdbi.v3.core.Jdbi;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.stream.Collectors;

public class ShowConnectionHandler implements JavalinHandler {
    private final Jdbi jdbi;
    private final ObjectMapper jackson;

    @Inject
    public ShowConnectionHandler(Jdbi jdbi, ObjectMapper jackson) {
        this.jdbi = jdbi;
        this.jackson = jackson;
    }

    @Override
    public void register(Javalin app) {
        final var selectConsumptionPointsSql = """
                                SELECT metering_point,
                       (start_date_time + (ord * INTERVAL '1 second' * CRS.METERING_INTERVAL_SECS))       AS start,
                       (start_date_time + ((ord + 1) * INTERVAL '1 second' * CRS.METERING_INTERVAL_SECS)) AS END_,
                       ORD,
                       CONSUMPTION,
                       METERING_TYPE
                FROM CONSUMPTION_RECORDS AS CRS
                         LEFT JOIN CONSUMPTION_POINTS DPS
                                   ON CRS.ID = DPS.CONSUMPTION_RECORD_ID
                WHERE CONNECTION_ID = ?
                  AND CONSUMPTION IS NOT NULL
                ORDER BY START_DATE_TIME, ORD
                """;
        app.get("/connections/{connectionId}", ctx -> {
            var connectionId = ctx.pathParam("connectionId");
            var consumptionRecordSummaries = jdbi.withHandle(h ->
                    h.createQuery("SELECT start_date_time || '##' || metering_interval_secs FROM consumption_records WHERE connection_id=?")
                            .bind(0, connectionId)
                            .mapTo(String.class)
                            .list());
            var dataPointsWithMeteringPoint = jdbi.withHandle(h ->
                    h.createQuery(selectConsumptionPointsSql).bind(0, connectionId)
                            .mapToMap()
                            .list());
            var dataPointMap = dataPointsWithMeteringPoint.stream()
                    .collect(Collectors.groupingBy(dpwmp -> dpwmp.get("metering_point")));
            var dataPointsForMeteringPointIdJson = jackson.writeValueAsString(dataPointMap);
            Map<String, Object> paramMap = Map.of(
                    "connectionId", connectionId,
                    "consumptionRecordSummaries", consumptionRecordSummaries,
                    "dataPointsForMeteringPointId", dataPointMap,
                    "dataPointsForMeteringPointIdJson", dataPointsForMeteringPointIdJson
            );
            ctx.render("show-connection.jte", paramMap);
        });
    }
}
