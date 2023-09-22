package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.*;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.RequestInfo;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import feign.FeignException;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

public class EnerginetRegionConnector implements RegionConnector {
    public static final String COUNTRY_CODE = "dk";
    public static final String MDA_CODE = COUNTRY_CODE + "-energinet";
    public static final String BASE_PATH = "/region-connectors/" + MDA_CODE;
    public static final String MDA_DISPLAY_NAME = "Denmark ENERGINET";
    public static final int COVERED_METERING_POINTS = 36951446; // TODO: Evaluate covered metering points
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetRegionConnector.class);
    private static final String CONNECTION_ID = "connectionId";

    final Sinks.Many<ConnectionStatusMessage> connectionStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
    private final EnerginetCustomerApiClient energinetCustomerApi;
    private final Javalin javalin = Javalin.create();
    private final EnerginetConfiguration configuration;
    private final ConcurrentMap<String, RequestInfo> permissionIdToRequestInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();

    public EnerginetRegionConnector(EnerginetConfiguration configuration, EnerginetCustomerApiClient energinetCustomerApi) {
        this.energinetCustomerApi = requireNonNull(energinetCustomerApi);
        this.configuration = requireNonNull(configuration);

        connectionStatusSink.asFlux().subscribe(connectionStatusMessage -> {
            var permissionId = connectionStatusMessage.permissionId();
            LOGGER.info("Received connectionStatusMessage for permissionId '{}': {}", permissionId, connectionStatusMessage);
            if (permissionId != null) {
                permissionIdToConnectionStatusMessages.put(permissionId, connectionStatusMessage);
            }
        });
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH + "/", COVERED_METERING_POINTS);
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(connectionStatusSink.asFlux());
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordSink.asFlux());
    }

    @Override
    public void terminatePermission(String permissionId) {
        String connectionId = Optional.ofNullable(permissionIdToRequestInfo.get(permissionId)).map(RequestInfo::connectionId).orElse(null);
        connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(connectionId, permissionId, PermissionProcessStatus.TERMINATED));
        // Nothing to implement yet, needs to be done after we support pulling data from the future
        throw new UnsupportedOperationException("revokePermission is not yet implemented");
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        JavalinValidation.register(ZonedDateTime.class, value -> value != null && !value.isBlank() ? LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneOffset.UTC) : null);

        javalin.get(BASE_PATH + "/ce.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public/ce.js")));
        });

        javalin.get(BASE_PATH + "/permission-status", ctx -> {
            var permissionId = ctx.queryParamAsClass("permissionId", String.class).get();
            var connectionStatusMessage = permissionIdToConnectionStatusMessages.get(permissionId);
            if (connectionStatusMessage == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }
            ctx.json(connectionStatusMessage);
        });

        javalin.post(BASE_PATH + "/permission-request", ctx -> {
            var connectionIdValidator = ctx.formParamAsClass(CONNECTION_ID, String.class).check(s -> s != null && !s.isBlank(), "connectionId must not be null or blank");

            connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(ctx.formParam(CONNECTION_ID), null, PermissionProcessStatus.CREATED));

            var refreshTokenValidator = ctx.formParamAsClass("refreshToken", String.class).check(Objects::nonNull, "refreshToken must not be null");
            var meteringPointValidator = ctx.formParamAsClass("meteringPoint", String.class).check(Objects::nonNull, "meteringPoint must not be null");
            var startValidator = ctx.formParamAsClass("start", ZonedDateTime.class).check(Objects::nonNull, "start must not be null");
            var endValidator = ctx.formParamAsClass("end", ZonedDateTime.class).check(end -> end == null || end.isAfter(startValidator.get()), "end must not be null and after start");

            var errors = JavalinValidation.collectErrors(connectionIdValidator, startValidator, endValidator);
            if (!errors.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(errors);
                return;
            }

            var permissionId = UUID.randomUUID().toString();
            connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(connectionIdValidator.get(), permissionId, PermissionProcessStatus.VALIDATED));

            var requestInfo = new RequestInfo(connectionIdValidator.get(), refreshTokenValidator.get(), meteringPointValidator.get(), startValidator.get(), endValidator.get());
            permissionIdToRequestInfo.put(permissionId, requestInfo);
            energinetCustomerApi.setRefreshToken(requestInfo.refreshToken());
            energinetCustomerApi.setUserCorrelationId(UUID.fromString(permissionId));

            MeteringPoints meteringPoints = new MeteringPoints();
            //meteringPoints.addMeteringPointItem("571313179100066516");
            meteringPoints.addMeteringPointItem(requestInfo.meteringPoint());
            MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);

            connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, "Access to data requested"));

            new Thread(() -> {
                try {
                    energinetCustomerApi.apiToken();
                    connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), PermissionProcessStatus.ACCEPTED, "Access to data granted"));
                } catch (FeignException e) {
                    LOGGER.error("Something went wrong while fetching token from Energinet:", e);
                }

                try {
                    var consumptionRecord = energinetCustomerApi.getTimeSeries(startValidator.get(), endValidator.get(), TimeSeriesAggregationEnum.ACTUAL, meteringPointsRequest);

                    consumptionRecord.setConnectionId(requestInfo.connectionId());
                    consumptionRecord.setPermissionId(permissionId);
                    consumptionRecordSink.tryEmitNext(consumptionRecord);
                } catch (FeignException e) {
                    LOGGER.error("Something went wrong while fetching data from Energinet:", e);
                }

            }).start();
        });


        javalin.exception(Exception.class, (e, ctx) -> {
            LOGGER.error("Exception occurred while processing request", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("Internal Server Error");
        });

        javalin.start(address.getHostName(), address.getPort());

        return javalin.port();
    }

    @Override
    public Map<String, HealthState> health() {
        return energinetCustomerApi.health();
    }

    @Override
    public void close() throws Exception {
        javalin.close();
        connectionStatusSink.tryEmitComplete();
        consumptionRecordSink.tryEmitComplete();
    }
}
