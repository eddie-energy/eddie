package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
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
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

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
    private final AtConfiguration atConfiguration;
    private final EdaAdapter edaAdapter;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final AtPermissionRequestRepository permissionRequestRepository;
    private final Javalin javalin = Javalin.create();

    /**
     * Used to send permission state messages.
     */
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks
            .many()
            .multicast()
            .onBackpressureBuffer();

    private final PermissionRequestFactory permissionRequestFactory;

    /**
     * Workaround because ws are currently not working
     */
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();

    public EdaRegionConnector(AtConfiguration atConfiguration, EdaAdapter edaAdapter, AtPermissionRequestRepository permissionRequestRepository) throws TransmissionException {
        requireNonNull(atConfiguration);
        requireNonNull(edaAdapter);
        requireNonNull(permissionRequestRepository);

        this.atConfiguration = atConfiguration;
        this.edaAdapter = edaAdapter;
        this.consumptionRecordMapper = new ConsumptionRecordMapper();
        this.permissionRequestFactory = new PermissionRequestFactory(edaAdapter, permissionStateMessages, permissionRequestRepository);
        this.permissionRequestRepository = permissionRequestRepository;

        edaAdapter.getCMRequestStatusStream()
                .subscribe(this::processIncomingCmStatusMessages);

        edaAdapter.start();
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                edaAdapter.getConsumptionRecordStream()
                        .mapNotNull(this::mapConsumptionRecordToCIMConsumptionRecord)
        );
    }

    private static PermissionProcessStatus getPermissionProcessStatus(CMRequestStatus cmRequestStatus, PermissionRequest request) throws FutureStateException, PastStateException {
        return switch (cmRequestStatus.getStatus()) {
            case ACCEPTED -> {
                request.accept();
                yield PermissionProcessStatus.ACCEPTED;
            }
            case ERROR -> {
                request.invalid();
                yield PermissionProcessStatus.INVALID;
            }
            case REJECTED -> {
                request.rejected();
                yield PermissionProcessStatus.REJECTED;
            }
            case RECEIVED -> {
                request.receivedPermissionAdministratorResponse();
                yield PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR;
            }
            case DELIVERED, SENT -> PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR;
        };
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
            var connectionIdValidator = ctx.formParamAsClass(CONNECTION_ID, String.class).check(s -> s != null && !s.isBlank(), "connectionId must not be null or blank");

            var meteringPointIdValidator = ctx.formParamAsClass("meteringPointId", String.class).check(s -> s != null && s.length() == 33, "meteringPointId must be 33 characters long");

            LocalDate now = LocalDate.now(ZoneId.of("Europe/Vienna"));
            var startValidator = ctx.formParamAsClass("start", LocalDate.class).check(Objects::nonNull, "start must not be null").check(start -> start.isAfter(now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST)), "start must not be older than 36 months");

            var endValidator = ctx.formParamAsClass("end", LocalDate.class)
                    //.allowNullable() // disable for now as we don't support Future data yet
                    .check(Objects::nonNull, "end must not be null")
                    .check(end -> end.isAfter(startValidator.get()), "end must be after start")
                    .check(end -> end.isBefore(now.minusDays(1)), "end must be in the past"); // for now, we only support historical data

            var errors = JavalinValidation.collectErrors(
                    connectionIdValidator,
                    meteringPointIdValidator,
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
            DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint(null, meteringPointIdValidator.get());

            var ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, new CCMOTimeFrame(start, end), this.atConfiguration, RequestDataType.METERING_DATA, // for now only allow metering data
                    AllowedMeteringIntervalType.QH, AllowedTransmissionCycle.D);


            var connectionId = connectionIdValidator.get();
            // Created State as root state
            AtPermissionRequest permissionRequest = permissionRequestFactory.create(connectionId, ccmoRequest);
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
     * Process a CMRequestStatus and emit a ConnectionStatusMessage if possible,
     * also adds connectionId and permissionId for identification
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

        var permissionId = optionalPermissionRequest.get().permissionId();
        var connectionId = optionalPermissionRequest.get().connectionId();

        var message = cmRequestStatus.getMessage();
        var now = ZonedDateTime.now(ZoneId.systemDefault());
        try {
            var status = getPermissionProcessStatus(cmRequestStatus, optionalPermissionRequest.get());
            var connectionStatusMessage = new ConnectionStatusMessage(connectionId, permissionId, now, status, message);
            // workaround because ws are currently not working
            permissionIdToConnectionStatusMessages.put(permissionId, connectionStatusMessage);

            permissionStateMessages.tryEmitNext(connectionStatusMessage);
        } catch (PastStateException | FutureStateException e) {
            permissionStateMessages.tryEmitError(e);
        }
    }

    /**
     * Map an EDA consumption record to a CIM consumption record
     * and add connectionId and permissionId for identification
     *
     * @param consumptionRecord the consumption record to process
     */
    private @Nullable ConsumptionRecord mapConsumptionRecordToCIMConsumptionRecord(at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.ConsumptionRecord consumptionRecord) {
        // map an EDA consumption record it to a CIM consumption record
        // and add connectionId and permissionId for identification
        String conversationId = consumptionRecord.getProcessDirectory().getConversationId();
        Optional<AtPermissionRequest> permissionRequest = permissionRequestRepository
                .findByConversationIdOrCMRequestId(conversationId, null);
        String permissionId = permissionRequest.map(PermissionRequest::permissionId).orElse(null);
        String connectionId = permissionRequest.map(PermissionRequest::connectionId).orElse(null);
        LOGGER.info("Received consumption record (ConversationId '{}') for permissionId {} and connectionId {}", conversationId, permissionId, connectionId);
        try {
            return consumptionRecordMapper.mapToCIM(consumptionRecord, permissionId, connectionId);
        } catch (InvalidMappingException e) {
            // TODO In the future this should also inform the administrative console about the invalid mapping
            LOGGER.error("Could not map consumption record to CIM consumption record", e);
            return null;
        }
    }
}
