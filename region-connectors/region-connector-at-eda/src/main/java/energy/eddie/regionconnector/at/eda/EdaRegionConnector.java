package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.at.api.RegionConnectorAT;
import energy.eddie.regionconnector.at.api.SendCCMORequestResult;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
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
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

public class EdaRegionConnector implements RegionConnectorAT {

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
    private final EdaIdMapper edaIdMapper;
    private final Javalin javalin = Javalin.create();

    /**
     * Used to send permission state messages.
     */
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks
            .many()
            .multicast()
            .onBackpressureBuffer();

    /**
     * Workaround because ws are currently not working
     */
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();

    public EdaRegionConnector(AtConfiguration atConfiguration, EdaAdapter edaAdapter, EdaIdMapper edaIdMapper) throws TransmissionException {
        requireNonNull(atConfiguration);
        requireNonNull(edaAdapter);
        requireNonNull(edaIdMapper);

        this.atConfiguration = atConfiguration;
        this.edaAdapter = edaAdapter;
        this.consumptionRecordMapper = new ConsumptionRecordMapper();
        this.edaIdMapper = edaIdMapper;

        edaAdapter.start();
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                edaAdapter.getConsumptionRecordStream()
                        .mapNotNull(this::mapConsumptionRecordToCIMConsumptionRecord)
        );
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        Flux<ConnectionStatusMessage> merged = edaAdapter
                .getCMRequestStatusStream()
                .mapNotNull(this::mapCMRequestStatusToConnectionStatusMessage)
                .mergeWith(permissionStateMessages.asFlux());
        return JdkFlowAdapter.publisherToFlowPublisher(merged);
    }

    @Override
    public void terminatePermission(String permissionId) {
        String connectionId = edaIdMapper
                .getMappingInfoForConsentId(permissionId)
                .map(MappingInfo::connectionId)
                .orElse(null);
        permissionStateMessages.tryEmitNext(
                new ConnectionStatusMessage(
                        connectionId,
                        permissionId,
                        PermissionProcessStatus.TERMINATED
                )
        );
        throw new UnsupportedOperationException("Revoke permission is not yet implemented");
    }

    @Override
    public SendCCMORequestResult sendCCMORequest(String connectionId, CMRequest request) throws TransmissionException, JAXBException {
        requireNonNull(connectionId);
        requireNonNull(request);
        var permissionId = UUID.randomUUID().toString();
        permissionStateMessages.tryEmitNext(new ConnectionStatusMessage(
                connectionId,
                permissionId,
                PermissionProcessStatus.VALIDATED)
        );
        edaIdMapper.addMappingInfo(request.getProcessDirectory().getConversationId(), request.getProcessDirectory().getCMRequestId(), new MappingInfo(permissionId, connectionId));

        edaAdapter.sendCMRequest(request);
        return new SendCCMORequestResult(permissionId, request.getProcessDirectory().getCMRequestId());
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
        javalin.before("/permission-request", ctx -> permissionStateMessages.tryEmitNext(
                new ConnectionStatusMessage(
                        ctx.formParam(CONNECTION_ID),
                        null,
                        PermissionProcessStatus.CREATED
                )
        ));
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

            var result = sendCCMORequest(connectionId, ccmoRequest.toCMRequest());
            ctx.status(HttpStatus.OK);
            ctx.json(result);
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
        edaAdapter.close();
        permissionStateMessages.tryEmitComplete();
    }

    /**
     * Map a CMRequestStatus to a ConnectionStatusMessage
     * and add connectionId and permissionId for identification
     *
     * @param cmRequestStatus the CMRequestStatus to process
     */
    private @Nullable ConnectionStatusMessage mapCMRequestStatusToConnectionStatusMessage(CMRequestStatus cmRequestStatus) {
        var mappingInfo = edaIdMapper.getMappingInfoForConversationIdOrRequestID(cmRequestStatus.getConversationId(), cmRequestStatus.getCMRequestId().orElse(null));
        if (mappingInfo.isEmpty()) {
            // should not happen if a persistent mapping is used
            // TODO inform the administrative console if it happens
            LOGGER.warn("Received CMRequestStatus for unknown conversationId or requestId: {}", cmRequestStatus);
            return null;
        }

        var permissionId = mappingInfo.get().permissionId();
        var connectionId = mappingInfo.get().connectionId();

        var message = cmRequestStatus.getMessage();
        var now = ZonedDateTime.now(ZoneId.systemDefault());

        var status = switch (cmRequestStatus.getStatus()) {
            case ACCEPTED -> PermissionProcessStatus.ACCEPTED;
            case ERROR -> PermissionProcessStatus.INVALID;
            case REJECTED -> PermissionProcessStatus.REJECTED;
            case SENT, RECEIVED, DELIVERED -> PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR;
        };
        var connectionStatusMessage = new ConnectionStatusMessage(connectionId, permissionId, now, status, message);
        // workaround because ws are currently not working
        permissionIdToConnectionStatusMessages.put(permissionId, connectionStatusMessage);

        return connectionStatusMessage;
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

        var mappingInfo = edaIdMapper.getMappingInfoForConversationIdOrRequestID(consumptionRecord.getProcessDirectory().getConversationId(), null);
        var permissionId = mappingInfo.map(MappingInfo::permissionId).orElse(null);
        var connectionId = mappingInfo.map(MappingInfo::connectionId).orElse(null);
        LOGGER.info("Received consumption record (ConversationId '{}') for permissionId {} and connectionId {}", consumptionRecord.getProcessDirectory().getConversationId(), permissionId, connectionId);
        try {
            return consumptionRecordMapper.mapToCIM(consumptionRecord, permissionId, connectionId);
        } catch (InvalidMappingException e) {
            // TODO In the future this should also inform the administrative console about the invalid mapping
            LOGGER.error("Could not map consumption record to CIM consumption record", e);
            return null;
        }
    }
}
