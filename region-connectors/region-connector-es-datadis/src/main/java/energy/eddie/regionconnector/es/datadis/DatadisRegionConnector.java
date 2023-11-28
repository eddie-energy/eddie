package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.*;
import static energy.eddie.regionconnector.es.datadis.utils.ParameterKeys.PERMISSION_ID_KEY;
import static java.util.Objects.requireNonNull;

public class DatadisRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider,
        Mvp1ConsumptionRecordProvider, AuthorizationResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisRegionConnector.class);

    private final Javalin javalin = Javalin.create();

    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks
            .many()
            .multicast()
            .onBackpressureBuffer();

    private final Sinks.Many<ConsumptionRecord> consumptionRecords = Sinks
            .many()
            .multicast()
            .onBackpressureBuffer();

    /**
     * Workaround because ws are currently not working
     */
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();
    private final PermissionRequestFactory permissionRequestFactory;
    private final EsPermissionRequestRepository permissionRequestRepository;
    private final DatadisScheduler datadisScheduler;


    public DatadisRegionConnector(DataApi dataApi, AuthorizationApi authorizationApi, EsPermissionRequestRepository permissionRequestRepository) {
        requireNonNull(dataApi);
        requireNonNull(authorizationApi);
        requireNonNull(permissionRequestRepository);

        this.datadisScheduler = new DatadisScheduler(dataApi, consumptionRecords, new ConsumptionRecordMapper());
        this.permissionRequestFactory = new PermissionRequestFactory(authorizationApi, permissionStateMessages, permissionRequestRepository, this);
        this.permissionRequestRepository = permissionRequestRepository;

        this.permissionStateMessages.asFlux().subscribe(connectionStatusMessage -> {
            permissionIdToConnectionStatusMessages.put(connectionStatusMessage.permissionId(), connectionStatusMessage);
            LOGGER.debug("Received connection status message {}", connectionStatusMessage);
        });
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH, COVERED_METERING_POINTS);
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(permissionStateMessages.asFlux());
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecords.asFlux());
    }

    @Override
    public void terminatePermission(String permissionId) {
        var permissionRequest = permissionRequestRepository.findByPermissionId(permissionId);
        if (permissionRequest.isEmpty()) {
            throw new IllegalStateException("No permission with this id found: %s".formatted(permissionId));
        }

        try {
            permissionRequest.get().terminate();
        } catch (StateTransitionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        JavalinValidation.register(ZonedDateTime.class, value -> value != null && !value.isBlank() ? LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay(ZONE_ID_SPAIN) : null);
        JavalinValidation.register(MeasurementType.class, MeasurementType::valueOf);

        javalin.get(BASE_PATH + "/ce.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            if (devMode) {
                context.result(new FileInputStream("./region-connectors/region-connector-es-datadis/src/main/resources/public" + BASE_PATH + "ce.js"));
            } else {
                context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public" + BASE_PATH + "ce.js")));
            }
        });

        javalin.get(BASE_PATH + "/permission-status", ctx -> {
            var permissionId = ctx.queryParamAsClass(PERMISSION_ID_KEY, String.class).get();
            var connectionStatusMessage = permissionIdToConnectionStatusMessages.get(permissionId);
            if (connectionStatusMessage == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }
            ctx.json(connectionStatusMessage);
        });

        javalin.post(BASE_PATH + "/permission-request", ctx -> {
            // Created State as root state
            PermissionRequest permissionRequest = permissionRequestFactory.create(ctx);
            permissionRequest.validate();
            try {
                permissionRequest.sendToPermissionAdministrator();
            } catch (PastStateException e) {
                // The request was malformed and there is nothing more to do.
                // The permission request itself will create the http response
            }
        });

        javalin.post(BASE_PATH + "/permission-request/accepted", ctx -> {
            var permissionId = ctx.queryParamAsClass(PERMISSION_ID_KEY, String.class).get();
            var request = permissionRequestRepository.findByPermissionId(permissionId);

            if (request.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            var permissionRequest = request.get();
            permissionRequest.accept();

            datadisScheduler.pullAvailableHistoricalData(permissionRequest);

            ctx.status(HttpStatus.OK);
        });


        javalin.post(BASE_PATH + "/permission-request/rejected", ctx -> {
            var permissionId = ctx.queryParamAsClass(PERMISSION_ID_KEY, String.class).get();
            var permissionRequest = permissionRequestRepository.findByPermissionId(permissionId);

            if (permissionRequest.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            permissionRequest.get().rejected();
            ctx.status(HttpStatus.OK);
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
        return Map.of(
                "permissionRequestRepository", HealthState.UP
        );
    }

    @Override
    public void close() {
        permissionStateMessages.tryEmitComplete();
        consumptionRecords.tryEmitComplete();

    }


    @Override
    public void handleAuthorizationRequestResponse(String permissionId, AuthorizationRequestResponse response) {
        var optionalPermissionRequest = permissionRequestRepository.findByPermissionId(permissionId);
        if (optionalPermissionRequest.isEmpty()) {
            LOGGER.error("Received response for unknown permission request {}", permissionId);
            return;
        }

        var permissionRequest = optionalPermissionRequest.get();
        try {
            permissionRequest.receivedPermissionAdministratorResponse();
            if (response == AuthorizationRequestResponse.NO_SUPPLIES || response == AuthorizationRequestResponse.NO_NIF) {
                permissionRequest.invalid();
            }
        } catch (StateTransitionException e) {
            LOGGER.error("Error changing state of permission request {}", permissionRequest, e);
        }
    }
}
