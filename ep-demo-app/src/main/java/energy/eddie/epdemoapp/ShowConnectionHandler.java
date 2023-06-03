package energy.eddie.epdemoapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.javalin.Javalin;
import org.jdbi.v3.core.Jdbi;

import java.util.Map;
import java.util.stream.Collectors;

public class ShowConnectionHandler implements JavalinHandler {
    @Inject
    private Jdbi jdbi;

    @Inject
    private ObjectMapper jackson;

    @Override
    public void register(Javalin app) {
        app.get("/connections/{connectionId}", ctx -> {
            var connectionId = ctx.pathParam("connectionId");
            var consumptionRecordSummaries = jdbi.withHandle(h ->
                    h.createQuery("select START_DATE_TIME || '##' || METERING_INTERVAL_SECS from CONSUMPTION_RECORDS where CONNECTION_ID=?")
                            .bind(0, connectionId)
                            .mapTo(String.class)
                            .list());
            var dataPointsWithMeteringPoint = jdbi.withHandle(h ->
                    h.createQuery("""
                                    select METERING_POINT,
                                           DATEADD(SECOND, ORD*crs.METERING_INTERVAL_SECS, START_DATE_TIME)     AS START,
                                           DATEADD(SECOND, (ORD+1)*crs.METERING_INTERVAL_SECS, START_DATE_TIME) AS END_,
                                           ORD,
                                           CONSUMPTION,
                                           METERING_TYPE
                                    from CONSUMPTION_RECORDS as crs
                                             left join CONSUMPTION_POINTS dps on crs.ID = dps.CONSUMPTION_RECORD_ID
                                    where CONNECTION_ID = ? and CONSUMPTION is not null
                                    order by START_DATE_TIME, ORD
                                    """).bind(0, connectionId)
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
