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
    public static final String COUNTRY_CODE = "fr";
    public static final String MDA_CODE = COUNTRY_CODE + "-enedis";
    public static final String BASE_PATH = "/region-connectors/" + MDA_CODE;
    public static final String MDA_DISPLAY_NAME = "France ENEDIS";
    public static final int COVERED_METERING_POINTS = 36951446;
    final Sinks.Many<ConnectionStatusMessage> connectionStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
    private final EnedisApi enedisApi;
    private final Logger logger = LoggerFactory.getLogger(EnedisRegionConnector.class);
    private final Javalin javalin = Javalin.create();
    private final EnedisConfiguration configuration;
    private final ConcurrentMap<String, RequestInfo> permissionIdToRequestInfo = new ConcurrentHashMap<>();

    public EnedisRegionConnector() throws IOException {
        Properties properties = new Properties();
        var in = EnedisCliClient.class.getClassLoader().getResourceAsStream("regionconnector-fr-enedis.properties");
        properties.load(in);

        this.configuration = new PropertiesEnedisConfiguration(properties);
        this.enedisApi = new EnedisApiClientDecorator(configuration);
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

        // Disabled for now as with the current proxy routing setup, the websocket endpoint can't be reached, instead for now we use a polling approach
        /*javalin.ws(BASE_PATH + "/permission-status", wsEndpoint -> wsEndpoint.onConnect(wsContext -> {
            var permissionId = wsContext.queryParam("permissionId");

                    Disposable subscribe = JdkFlowAdapter.flowPublisherToFlux(getConnectionStatusMessageStream())
                            .filter(connectionStatusMessage -> connectionStatusMessage.permissionId().equals(permissionId))
                            .subscribe(wsContext::send);

                    logger.info("New connection to websocket endpoint from SessionId '{}'for PermissionId '{}'", wsContext.getSessionId(), permissionId);

                    wsEndpoint.onClose(context -> {
                        subscribe.dispose();
                        logger.info("Closed connection to websocket endpoint from SessionId '{}'for PermissionId '{}'", wsContext.getSessionId(), permissionId);
                    });
                }))*/
        javalin.get(BASE_PATH + "/permission-status", ctx -> {
            var permissionId = ctx.queryParamAsClass("permissionId", String.class).get();
            var requestInfo = permissionIdToRequestInfo.get(permissionId);
            if (requestInfo == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }
            ctx.json(requestInfo);
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

            var redirectUri = "https://mon-compte-particulier.enedis.fr/dataconnect/v1/oauth2/authorize" +
                    "?client_id=" + configuration.clientId() +
                    "?response_type=code" +
                    "?state=" + permissionId +
                    "?duration=" + "P1Y"; // TODO move to config

            ctx.json(Map.of("permissionId", permissionId, "redirectUri", redirectUri));

            connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), ConnectionStatusMessage.Status.REQUESTED, "Access to data requested"));
        });

        javalin.get(BASE_PATH + "/authorization-callback", ctx -> {
            // TODO implement non happy path
            var permissionId = ctx.queryParam("state");
            if (permissionId == null || !permissionIdToRequestInfo.containsKey(permissionId)) {
                // unknown state / permissionId => not coming / initiated by our frontend
                logger.warn("authorization-callback called with unknown state '{}'", permissionId);
                ctx.status(HttpStatus.BAD_REQUEST);
                return;
            }

            var requestInfo = permissionIdToRequestInfo.get(permissionId);
            if (requestInfo == null) {
                logger.warn("authorization-callback called with unknown state (permissionId) '{}', can't find requestInfo", permissionId);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }

            var usagePointId = ctx.queryParam("usage_point_id");
            if (usagePointId == null) { // probably when request was denied
                // TODO when ENEDIS authorization api is up again, look what happens if request gets denied and get message
                connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), ConnectionStatusMessage.Status.REJECTED, "Access to data rejected"));
                ctx.redirect("error dont know where to redirect to");
                return;
            }

            connectionStatusSink.tryEmitNext(new ConnectionStatusMessage(requestInfo.connectionId(), permissionId, ZonedDateTime.now(requestInfo.start().getZone()), ConnectionStatusMessage.Status.GRANTED, "Access to data granted"));

            try {
                // TODO should be done in the background
                // request data from enedis
                enedisApi.postToken(); // fetch jwt token
                var consumptionRecord = enedisApi.getConsumptionLoadCurve(usagePointId, requestInfo.start(), requestInfo.end());
                // map ids
                consumptionRecord.setConnectionId(requestInfo.connectionId());
                consumptionRecord.setPermissionId(permissionId);
                // publish
                consumptionRecordSink.tryEmitNext(consumptionRecord);

                ctx.redirect("back to microfrontend");
            } catch (ApiException e) {
                // TODO map errors and publish messages
                logger.error("Something went wrong while fetching data from ENEDIS:", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.result("Internal Server Error"); // TODO maybe redirect somewhere else
            }
        });

        javalin.exception(Exception.class, (e, ctx) -> {
            logger.error("Exception occurred while processing request", e);
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
