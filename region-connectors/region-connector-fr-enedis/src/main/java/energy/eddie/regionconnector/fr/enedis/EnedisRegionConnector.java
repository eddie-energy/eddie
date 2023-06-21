package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientDecorator;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.PropertiesEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

public class EnedisRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisRegionConnector.class);
    public static final String COUNTRY_CODE = "fr";
    public static final String MDA_CODE = COUNTRY_CODE + "-enedis";
    public static final String BASE_PATH = "/region-connectors/" + MDA_CODE;
    public static final String MDA_DISPLAY_NAME = "France ENEDIS";
    public static final int COVERED_METERING_POINTS = 36951446;
    final Sinks.Many<ConnectionStatusMessage> connectionStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
    private final EnedisApi enedisApi;
    private final Javalin javalin = Javalin.create();
    private final EnedisConfiguration configuration;
    private final ConcurrentMap<String, RequestInfo> permissionIdToRequestInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();


    public EnedisRegionConnector() throws IOException {
        Properties properties = new Properties();
        var in = EnedisCliClient.class.getClassLoader().getResourceAsStream("regionconnector-fr-enedis.properties");
        properties.load(in);

        this.configuration = new PropertiesEnedisConfiguration(properties);
        this.enedisApi = new EnedisApiClientDecorator(configuration);

        connectionStatusSink.asFlux().subscribe(connectionStatusMessage -> {
            var permissionId = connectionStatusMessage.permissionId();
            if (permissionId != null) {
                permissionIdToConnectionStatusMessages.put(permissionId, connectionStatusMessage);
            }
        });
    }

    public EnedisRegionConnector(EnedisConfiguration configuration, EnedisApi enedisApi) {
        requireNonNull(configuration);
        requireNonNull(enedisApi);

        this.configuration = configuration;
        this.enedisApi = enedisApi;
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
    public void revokePermission(String permissionId) {
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
            // TODO rework validation after mvp1
            var connectionIdValidator = ctx.formParamAsClass("connectionId", String.class)
                    .check(s -> s != null && !s.isBlank(), "connectionId must not be null or blank");

            var startValidator = ctx.formParamAsClass("start", ZonedDateTime.class)
                    .check(Objects::nonNull, "start must not be null");
            var endValidator = ctx.formParamAsClass("end", ZonedDateTime.class)
                    .check(end -> end == null || end.isAfter(startValidator.get()), "end must not be null and after start");

            var errors = JavalinValidation.collectErrors(connectionIdValidator, startValidator, endValidator);
            if (!errors.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(errors);
                return;
            }

            var permissionId = UUID.randomUUID().toString();
            var requestInfo = new RequestInfo(connectionIdValidator.get(), startValidator.get(), endValidator.get());
            permissionIdToRequestInfo.put(permissionId, requestInfo);
            
            var redirectUri = new URIBuilder()
                    .setScheme("https")
                    .setHost("mon-compte-particulier.enedis.fr")
                    .setPath("/dataconnect/v1/oauth2/authorize")
                    .addParameter("client_id", configuration.clientId())
                    .addParameter("response_type", "code")
                    .addParameter("state", permissionId)
                    .addParameter("duration", "P1Y") // TODO move to config
                    .build();

            ctx.json(Map.of("permissionId", permissionId, "redirectUri", redirectUri.toString()));

            connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), ConnectionStatusMessage.Status.REQUESTED, "Access to data requested"));
        });

        javalin.get(BASE_PATH + "/authorization-callback", ctx -> {
            // TODO implement non happy path
            var permissionId = ctx.queryParam("state");
            if (permissionId == null || !permissionIdToRequestInfo.containsKey(permissionId)) {
                // unknown state / permissionId => not coming / initiated by our frontend
                LOGGER.warn("authorization-callback called with unknown state '{}'", permissionId);
                ctx.status(HttpStatus.BAD_REQUEST);
                return;
            }

            var requestInfo = permissionIdToRequestInfo.get(permissionId);
            if (requestInfo == null) {
                LOGGER.warn("authorization-callback called with unknown state (permissionId) '{}', can't find requestInfo", permissionId);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }

            var usagePointId = ctx.queryParam("usage_point_id");
            if (usagePointId == null || ctx.status() == HttpStatus.FORBIDDEN) { // probably when request was denied
                connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), ConnectionStatusMessage.Status.REJECTED, "Access to data rejected"));
                ctx.html("<h1>Access to data denied, you can close this window.</h1>");
                return;
            }

            connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), ConnectionStatusMessage.Status.GRANTED, "Access to data granted"));
            ctx.html("<h1>Access to data granted, you can close this window.</h1>");

            new Thread(() -> {
                // TODO rework the retry logic after mvp1
                try {
                    enedisApi.postToken(); // fetch jwt token
                } catch (ApiException e) {
                    LOGGER.error("Something went wrong while fetching token from ENEDIS:", e);
                }
                // request data from enedis
                var start = requestInfo.start();
                var end = requestInfo.end();
                var tryCount = 0;
                // the api allows for a maximum of 7 days per request, so we need to split the request
                while (start.isBefore(end)) {
                    try {
                        var endOfRequest = start.plusDays(6); // including the start date, so 7 days
                        if (endOfRequest.isAfter(end)) {
                            endOfRequest = end;
                        }
                        LOGGER.info("Fetching data from ENEDIS for usage_point '{}' from '{}' to '{}'", usagePointId, start, endOfRequest);
                        var consumptionRecord = enedisApi.getConsumptionLoadCurve(usagePointId, start, endOfRequest);
                        // map ids
                        consumptionRecord.setConnectionId(requestInfo.connectionId());
                        consumptionRecord.setPermissionId(permissionId);
                        // publish
                        consumptionRecordSink.tryEmitNext(consumptionRecord);
                        start = endOfRequest;
                    } catch (ApiException e) {
                        // TODO map errors and publish messages
                        LOGGER.error("Something went wrong while fetching data from ENEDIS:", e);
                        if (tryCount++ > 10) {
                            LOGGER.error("Too many retries, giving up");
                            return;
                        }
                        try {
                            Thread.sleep(1200);
                        } catch (InterruptedException interruptedException) {
                            LOGGER.error("Interrupted while sleeping", interruptedException);
                            return;
                        }
                    }
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
    public void close() throws Exception {
        javalin.close();
    }
}
