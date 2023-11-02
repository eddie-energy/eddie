package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint.DSO_ID_LENGTH;
import static java.util.Objects.requireNonNull;

public class EdaRegionConnector implements RegionConnector {

    public static final String COUNTRY_CODE = "at";
    public static final String MDA_CODE = COUNTRY_CODE + "-eda";
    public static final String MDA_DISPLAY_NAME = "Austria EDA";
    /**
     * The base path of the region connector. COUNTRY_CODE is enough, as in austria we only need one region connector
     */
    public static final String BASE_PATH = "/region-connectors/at-eda/";
    /**
     * The number of metering points covered by EDA, i.e. all metering points in Austria
     */
    public static final int COVERED_METERING_POINTS = 5977915;
    /**
     * DSOs in Austria are only allowed to store data for the last 36 months
     */
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 36;
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRegionConnector.class);
    private static final String CONNECTION_ID = "connectionId";
    private static final String DATA_NEED_ID = "dataNeedId";
    private static final String METERING_POINT_ID = "meteringPointId";
    private static final String DSO_ID = "dsoId";
    private final AtConfiguration atConfiguration;
    private final EdaAdapter edaAdapter;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final AtPermissionRequestRepository permissionRequestRepository;
    private final Javalin javalin = Javalin.create();

    /**
     * Used to send permission state messages.
     */
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;

    private final PermissionRequestFactory permissionRequestFactory;

    /**
     * Workaround because ws are currently not working
     */
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();

    public EdaRegionConnector(AtConfiguration atConfiguration, EdaAdapter edaAdapter, AtPermissionRequestRepository permissionRequestRepository) throws TransmissionException {
        this(atConfiguration, edaAdapter, permissionRequestRepository, Sinks.many().multicast().onBackpressureBuffer());
    }

    EdaRegionConnector(AtConfiguration atConfiguration, EdaAdapter edaAdapter, AtPermissionRequestRepository permissionRequestRepository, Sinks.Many<ConnectionStatusMessage> permissionStateMessages) throws TransmissionException {
        this(atConfiguration, edaAdapter, permissionRequestRepository, new PermissionRequestFactory(edaAdapter, permissionStateMessages, permissionRequestRepository), permissionStateMessages);
    }

    EdaRegionConnector(AtConfiguration atConfiguration, EdaAdapter edaAdapter, AtPermissionRequestRepository permissionRequestRepository, PermissionRequestFactory permissionRequestFactory, Sinks.Many<ConnectionStatusMessage> permissionStateMessages) throws TransmissionException {
        requireNonNull(atConfiguration);
        requireNonNull(edaAdapter);
        requireNonNull(permissionRequestRepository);
        requireNonNull(permissionRequestFactory);
        requireNonNull(permissionStateMessages);

        this.atConfiguration = atConfiguration;
        this.edaAdapter = edaAdapter;
        this.consumptionRecordMapper = new ConsumptionRecordMapper();
        this.permissionRequestFactory = permissionRequestFactory;
        this.permissionStateMessages = permissionStateMessages;
        this.permissionRequestRepository = permissionRequestRepository;

        edaAdapter.getCMRequestStatusStream()
                .subscribe(this::processIncomingCmStatusMessages);

        edaAdapter.start();
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                edaAdapter.getConsumptionRecordStream()
                        .mapNotNull(this::mapConsumptionRecordToCIM)
                        .flatMap(this::emitForEachPermissionRequest)
        );
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(permissionStateMessages.asFlux());
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH, COVERED_METERING_POINTS);
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        JavalinValidation.register(LocalDate.class, value -> value != null && !value.isBlank() ? LocalDate.parse(value, DateTimeFormatter.ISO_DATE) : null);

        javalin.get(BASE_PATH + "/ce.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            if (devMode) {
                context.result(new FileInputStream("./region-connectors/region-connector-at-eda/src/main/resources/public/ce.js"));
            } else {
                context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public/ce.js")));
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
            var connectionIdValidator = ctx.formParamAsClass(CONNECTION_ID, String.class).check(s -> s != null && !s.isBlank(), "connectionId must not be null or blank");
            var dataNeedIdValidator = ctx.formParamAsClass(DATA_NEED_ID, String.class).check(s -> s != null && !s.isBlank(), "dataNeedId must not be null or blank");
            var meteringPointIdValidator = ctx.formParamAsClass(METERING_POINT_ID, String.class)
                    .allowNullable()
                    .check(s -> s == null || s.length() == 33, "meteringPointId must be 33 characters long");
            var dsoIdValidator = ctx.formParamAsClass(DSO_ID, String.class)
                    .allowNullable()
                    .check(s -> s == null || s.length() == DSO_ID_LENGTH, "dsoId must be " + DSO_ID_LENGTH + " characters long");

            LocalDate now = LocalDate.now(ZoneId.of("Europe/Vienna"));
            var startValidator = ctx.formParamAsClass("start", LocalDate.class).check(Objects::nonNull, "start must not be null").check(start -> start.isAfter(now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST)), "start must not be older than 36 months");

            var endValidator = ctx.formParamAsClass("end", LocalDate.class)
                    .check(Objects::nonNull, "end must not be null")
                    .check(end -> !startValidator.errors().isEmpty() ||
                                    end.isAfter(startValidator.get()),
                            "end must be after start")
                    .check(end -> !startValidator.errors().isEmpty() ||
                                    (startValidator.get().isBefore(now) && end.isBefore(now)) ||
                                    !startValidator.get().isBefore(now),
                            "end and start must either be completely in the past or completely in the future"
                    );

            var errors = JavalinValidation.collectErrors(
                    connectionIdValidator,
                    dataNeedIdValidator,
                    meteringPointIdValidator,
                    dsoIdValidator,
                    startValidator,
                    endValidator
            );

            if (!errors.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(errors);
                return;
            }

            var start = startValidator.get();
            var end = Objects.requireNonNullElseGet(endValidator.get(), () -> now.minusDays(1));
            DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint(dsoIdValidator.get(), meteringPointIdValidator.get());

            var ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, new CCMOTimeFrame(start, end), this.atConfiguration, RequestDataType.METERING_DATA, // for now only allow metering data
                    AllowedMeteringIntervalType.QH, AllowedTransmissionCycle.D);


            var connectionId = connectionIdValidator.get();
            // Created State as root state
            AtPermissionRequest permissionRequest = permissionRequestFactory.create(connectionId, dataNeedIdValidator.get(), ccmoRequest);
            permissionRequestRepository.save(permissionRequest);
            permissionRequest.validate();
            permissionRequest.sendToPermissionAdministrator();
            ctx.status(HttpStatus.OK);
            ctx.json(new energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequest(permissionRequest));
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
        return edaAdapter.health();
    }


    @Override
    public void close() throws Exception {
        javalin.close();
        edaAdapter.close();
        permissionStateMessages.tryEmitComplete();
    }

    @Override
    public void terminatePermission(String permissionId) {
        var request = permissionRequestRepository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            throw new IllegalStateException("No permission with this id found: %s".formatted(permissionId));
        }
        try {
            request.get().terminate();
        } catch (FutureStateException | PastStateException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Process a CMRequestStatus and change the state of the corresponding permission request accordingly
     *
     * @param cmRequestStatus the CMRequestStatus to process
     */
    private void processIncomingCmStatusMessages(CMRequestStatus cmRequestStatus) {
        var optionalPermissionRequest = permissionRequestRepository.findByConversationIdOrCMRequestId(
                cmRequestStatus.getConversationId(),
                cmRequestStatus.getCMRequestId().orElse(null)
        );

        if (optionalPermissionRequest.isEmpty()) {
            // should not happen if a persistent mapping is used
            // TODO inform the administrative console if it happens
            LOGGER.warn("Received CMRequestStatus for unknown conversationId or requestId: {}", cmRequestStatus);
            return;
        }

        var permissionRequest = optionalPermissionRequest.get();

        try {
            switch (cmRequestStatus.getStatus()) {
                case ACCEPTED -> {
                    if (permissionRequest.meteringPointId().isEmpty()) {
                        permissionRequest.setMeteringPointId(cmRequestStatus.getMeteringPoint()
                                .orElseThrow(() -> new IllegalStateException("This should never happen! Metering point id is missing in ACCEPTED CMRequestStatus message")));
                    }
                    permissionRequest.accept();
                }
                case ERROR -> permissionRequest.invalid();
                case REJECTED -> permissionRequest.rejected();
                case RECEIVED -> permissionRequest.receivedPermissionAdministratorResponse();
                default -> {
                    // Other CMRequestStatus do not change the state of the permission request,
                    // because they have no matching state in the consent process model
                }
            }
        } catch (PastStateException | FutureStateException e) {
            permissionStateMessages.tryEmitError(e);
        }

        updatePermissionIdToConnectionStatusMap(cmRequestStatus, permissionRequest);
    }

    /**
     * This method updates the permissionIdToConnectionStatusMessages map.
     * This is a workaround because our current proxy setup does not support websockets.
     * TODO remove this workaround when websockets are supported, replace it by a subscription to the permissionStateMessages that pushes the messages to the clients
     */
    private void updatePermissionIdToConnectionStatusMap(CMRequestStatus cmRequestStatus, AtPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        var connectionId = permissionRequest.connectionId();
        var dataNeedId = permissionRequest.dataNeedId();
        var message = cmRequestStatus.getMessage(); // we should consider adding this as a property to the AtPermissionRequest
        var now = ZonedDateTime.now(ZoneId.systemDefault());

        var connectionStatusMessage = new ConnectionStatusMessage(connectionId, permissionId, dataNeedId, now, permissionRequest.state().status(), message);
        permissionIdToConnectionStatusMessages.put(permissionId, connectionStatusMessage);
    }

    private @Nullable ConsumptionRecord mapConsumptionRecordToCIM(at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord consumptionRecord) {
        try {
            return consumptionRecordMapper.mapToCIM(consumptionRecord);
        } catch (InvalidMappingException e) {
            LOGGER.error("Could not map consumption record to CIM consumption record", e);
            return null;
        }
    }

    /**
     * Emit a {@link ConsumptionRecord} for each {@link AtPermissionRequest} that matches the {@link ConsumptionRecord#getMeteringPoint()} and {@link ConsumptionRecord#getStartDateTime()} of the given {@link ConsumptionRecord}
     *
     * @param consumptionRecord the consumption record to emit for each permission request
     */
    private Flux<ConsumptionRecord> emitForEachPermissionRequest(ConsumptionRecord consumptionRecord) {
        var permissionRequests = permissionRequestRepository.findByMeteringPointIdAndDate(
                consumptionRecord.getMeteringPoint(),
                consumptionRecord.getStartDateTime().toLocalDate()
        );

        if (permissionRequests.isEmpty()) {
            LOGGER.warn("No permission requests found for consumption record {}", consumptionRecord);
            return Flux.empty(); // Return an empty Flux if no permission requests are found
        }

        return Flux.fromIterable(permissionRequests).map(permissionRequest -> {
            consumptionRecord.setPermissionId(permissionRequest.permissionId());
            consumptionRecord.setConnectionId(permissionRequest.connectionId());
            consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
            return consumptionRecord;
        });
    }

}
