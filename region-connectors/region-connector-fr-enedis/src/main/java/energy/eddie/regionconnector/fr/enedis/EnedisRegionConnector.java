package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.*;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.shared.utils.ZonedDateTimeConverter;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

public class EnedisRegionConnector implements RegionConnector {
    public static final String COUNTRY_CODE = "fr";
    public static final String MDA_CODE = COUNTRY_CODE + "-enedis";
    public static final String BASE_PATH = "/region-connectors/fr-enedis/";
    public static final String MDA_DISPLAY_NAME = "France ENEDIS";
    public static final int COVERED_METERING_POINTS = 36951446;
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisRegionConnector.class);
    final Sinks.Many<ConnectionStatusMessage> connectionStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
    private final EnedisApi enedisApi;
    private final Javalin javalin = Javalin.create();
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();
    private final PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository;
    private final PermissionRequestFactory permissionRequestFactory;

    public EnedisRegionConnector(EnedisConfiguration configuration, EnedisApi enedisApi, PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository) {
        requireNonNull(configuration);
        requireNonNull(enedisApi);
        requireNonNull(permissionRequestRepository);

        this.enedisApi = enedisApi;
        this.permissionRequestRepository = permissionRequestRepository;
        this.permissionRequestFactory = new PermissionRequestFactory(permissionRequestRepository, connectionStatusSink, configuration);

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
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH, COVERED_METERING_POINTS);
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
        var permissionRequest = permissionRequestRepository.findByPermissionId(permissionId);
        if (permissionRequest.isEmpty()) {
            return;
        }
        try {
            permissionRequest.get().terminate();
        } catch (FutureStateException | PastStateException e) {
            LOGGER.error("PermissionRequest with permissionID {} cannot be revoked", permissionId, e);
        }
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        ZonedDateTimeConverter.register();
        javalin.get(BASE_PATH + "/ce.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            if (devMode) {
                context.result(new FileInputStream("./region-connectors/region-connector-fr-enedis/src/main/resources/public" + BASE_PATH + "ce.js"));
            } else {
                context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public" + BASE_PATH + "ce.js")));
            }
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
            PermissionRequest permissionRequest = permissionRequestFactory.create(ctx);
            permissionRequest.validate();
            try {
                permissionRequest.sendToPermissionAdministrator();
            } catch (PastStateException ignored) {
                // The request was malformed and there is nothing more to do.
                // The permission request itself will create the http response
            }
        });

        javalin.get(BASE_PATH + "/authorization-callback", ctx -> {
            // TODO implement non happy path
            var permissionId = ctx.queryParam("state");
            Optional<TimeframedPermissionRequest> optionalPermissionRequest = permissionRequestRepository.findByPermissionId(permissionId);
            if (optionalPermissionRequest.isEmpty()) {
                // unknown state / permissionId => not coming / initiated by our frontend
                LOGGER.warn("authorization-callback called with unknown state '{}'", permissionId);
                ctx.status(HttpStatus.BAD_REQUEST);
                return;
            }

            TimeframedPermissionRequest permissionRequest = optionalPermissionRequest.get();
            permissionRequest.receivedPermissionAdministratorResponse();
            var usagePointId = ctx.queryParam("usage_point_id");
            if (usagePointId == null || ctx.status() == HttpStatus.FORBIDDEN) { // probably when request was denied
                permissionRequest.rejected();
                ctx.html("<h1>Access to data denied, you can close this window.</h1>");
                return;
            }

            permissionRequest.accept();
            ctx.html("<h1>Access to data granted, you can close this window.</h1>");
            new Thread(() -> {
                // TODO rework the retry logic after mvp1
                try {
                    enedisApi.postToken(); // fetch jwt token
                } catch (ApiException e) {
                    LOGGER.error("Something went wrong while fetching token from ENEDIS:", e);
                }
                // request data from enedis
                var start = permissionRequest.start();
                var end = permissionRequest.end();
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
                        consumptionRecord.setConnectionId(permissionRequest.connectionId());
                        consumptionRecord.setPermissionId(permissionId);
                        consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
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
    public Map<String, HealthState> health() {
        return enedisApi.health();
    }

    @Override
    public void close() {
        javalin.close();
        connectionStatusSink.tryEmitComplete();
        consumptionRecordSink.tryEmitComplete();
    }
}