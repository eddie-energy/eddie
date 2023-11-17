package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.utils.PeriodResolutionEnumConverter;
import energy.eddie.regionconnector.shared.utils.ZonedDateTimeConverter;
import feign.FeignException;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

public class EnerginetRegionConnector implements RegionConnector {
    private static final String COUNTRY_CODE = "dk";
    private static final String MDA_CODE = COUNTRY_CODE + "-energinet";
    private static final String BASE_PATH = "/region-connectors/dk-energinet/";
    private static final String MDA_DISPLAY_NAME = "Denmark ENERGINET";
    private static final int COVERED_METERING_POINTS = 36951446; // TODO: Evaluate covered metering points
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetRegionConnector.class);

    final Sinks.Many<ConnectionStatusMessage> connectionStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
    private final EnerginetCustomerApi energinetCustomerApi;
    private final Javalin javalin = Javalin.create();
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();
    private final DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository;
    private final PermissionRequestFactory permissionRequestFactory;

    public EnerginetRegionConnector(EnerginetConfiguration configuration, EnerginetCustomerApi energinetCustomerApi, DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository) {
        this.energinetCustomerApi = requireNonNull(energinetCustomerApi);
        this.permissionRequestRepository = requireNonNull(permissionRequestRepository);
        this.permissionRequestFactory = new PermissionRequestFactory(permissionRequestRepository, connectionStatusSink, requireNonNull(configuration));

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
        PeriodResolutionEnumConverter.register();

        javalin.get(BASE_PATH + "/ce.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public" + BASE_PATH + "ce.js")));
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
            DkEnerginetCustomerPermissionRequest permissionRequest = permissionRequestFactory.create(ctx);
            permissionRequest.validate();
            try {
                permissionRequest.sendToPermissionAdministrator();
            } catch (PastStateException ignored) {
                // The given refresh token for the API is not valid -> therefore no consent was given
                permissionRequest.receivedPermissionAdministratorResponse();
                permissionRequest.rejected();
                return;
            }

            permissionRequest.receivedPermissionAdministratorResponse();
            energinetCustomerApi.setRefreshToken(permissionRequest.refreshToken());
            energinetCustomerApi.setUserCorrelationId(UUID.fromString(permissionRequest.permissionId()));
            MeteringPoints meteringPoints = new MeteringPoints();
            meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
            MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);

            permissionRequest.accept();

            new Thread(() -> {
                try {
                    energinetCustomerApi.apiToken();
                } catch (FeignException e) {
                    LOGGER.error("Something went wrong while fetching token from Energinet:", e);
                }

                try {
                    var consumptionRecord = energinetCustomerApi.getTimeSeries(
                            permissionRequest.start(),
                            permissionRequest.end(),
                            permissionRequest.periodResolution(),
                            meteringPointsRequest
                    );

                    consumptionRecord.setConnectionId(permissionRequest.connectionId());
                    consumptionRecord.setPermissionId(permissionRequest.permissionId());
                    consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
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
