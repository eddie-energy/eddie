package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.processing.v0_82.ConsumptionRecordProcessor;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class EdaRegionConnector implements
        energy.eddie.api.v0.RegionConnector,
        energy.eddie.api.v0_82.RegionConnector {

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
    private final EdaAdapter edaAdapter;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final ConsumptionRecordProcessor consumptionRecordProcessor;
    private final PermissionRequestService permissionRequestService;

    /**
     * Used to send permission state messages.
     */
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;
    private final Supplier<Integer> port;

    public EdaRegionConnector(EdaAdapter edaAdapter, PermissionRequestService permissionRequestService, ConsumptionRecordProcessor consumptionRecordProcessor) throws TransmissionException {
        this(
                edaAdapter,
                permissionRequestService,
                consumptionRecordProcessor,
                Sinks.many()
                        .multicast()
                        .onBackpressureBuffer(),
                () -> 0
        );
    }

    public EdaRegionConnector(EdaAdapter edaAdapter, PermissionRequestService permissionRequestService, ConsumptionRecordProcessor consumptionRecordProcessor, Sinks.Many<ConnectionStatusMessage> permissionStateMessages, Supplier<Integer> port) throws TransmissionException {
        requireNonNull(edaAdapter);
        requireNonNull(permissionRequestService);
        requireNonNull(consumptionRecordProcessor);
        requireNonNull(permissionStateMessages);

        this.edaAdapter = edaAdapter;
        this.consumptionRecordMapper = new ConsumptionRecordMapper();
        this.permissionRequestService = permissionRequestService;
        this.consumptionRecordProcessor = consumptionRecordProcessor;
        this.permissionStateMessages = permissionStateMessages;

        edaAdapter.getCMRequestStatusStream()
                .subscribe(this::processIncomingCmStatusMessages);

        edaAdapter.start();
        this.port = port;
    }

    private static void transitionPermissionRequest(CMRequestStatus cmRequestStatus, AtPermissionRequest request)
            throws StateTransitionException {
        switch (cmRequestStatus.getStatus()) {
            case ACCEPTED -> {
                if (request.meteringPointId().isEmpty()) {
                    cmRequestStatus.getMeteringPoint()
                            .ifPresentOrElse(
                                    request::setMeteringPointId,
                                    () -> {
                                        throw new IllegalStateException("Metering point id is missing in ACCEPTED CMRequestStatus message for CMRequest: " + request.cmRequestId());
                                    }
                            );
                }
                request.accept();
            }
            case ERROR -> request.invalid();
            case REJECTED -> request.rejected();
            case RECEIVED -> request.receivedPermissionAdministratorResponse();
            default -> {
                // Other CMRequestStatus do not change the state of the permission request,
                // because they have no matching state in the consent process model
            }
        }
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                edaAdapter.getConsumptionRecordStream()
                        .mapNotNull(this::mapConsumptionRecordToCIMConsumptionRecord)
                        .flatMap(this::emitForEachPermissionRequest)
        );
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(permissionStateMessages.asFlux());
    }

    @Override
    public Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                consumptionRecordProcessor.getEddieValidatedHistoricalDataMarketDocumentStream()
        );
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH, COVERED_METERING_POINTS);
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        return port.get();
    }

    @Override
    public Map<String, HealthState> health() {
        return edaAdapter.health();
    }

    @Override
    public void close() throws Exception {
        edaAdapter.close();
        permissionStateMessages.tryEmitComplete();
    }

    @Override
    public void terminatePermission(String permissionId) {
        var request = permissionRequestService.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            throw new IllegalStateException("No permission with this id found: %s".formatted(permissionId));
        }
        try {
            request.get().terminate();
        } catch (StateTransitionException e) {
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
        var optionalPermissionRequest = permissionRequestService.findByConversationIdOrCMRequestId(
                cmRequestStatus.getConversationId(),
                cmRequestStatus.getCMRequestId().orElse(null)
        );
        if (optionalPermissionRequest.isEmpty()) {
            // should not happen if a persistent mapping is used
            // TODO inform the administrative console if it happens
            LOGGER.warn("Received CMRequestStatus for unknown conversationId or requestId: {}", cmRequestStatus);
            return;
        }
        try {
            transitionPermissionRequest(cmRequestStatus, optionalPermissionRequest.get());
        } catch (IllegalStateException | StateTransitionException e) {
            permissionStateMessages.tryEmitError(e);
        }
    }

    /**
     * Map an EDA consumption record to a CIM consumption record
     * and add connectionId and permissionId for identification
     *
     * @param consumptionRecord the consumption record to process
     */
    private @Nullable ConsumptionRecord mapConsumptionRecordToCIMConsumptionRecord(at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord consumptionRecord) {
        // map an EDA consumption record it to a CIM consumption record
        // and add connectionId and permissionId for identification
        String conversationId = consumptionRecord.getProcessDirectory().getConversationId();
        Optional<AtPermissionRequest> permissionRequest = permissionRequestService
                .findByConversationIdOrCMRequestId(conversationId, null);
        String permissionId = permissionRequest.map(PermissionRequest::permissionId).orElse(null);
        String connectionId = permissionRequest.map(PermissionRequest::connectionId).orElse(null);
        LOGGER.info("Received consumption record (ConversationId '{}') for permissionId {} and connectionId {}", conversationId, permissionId, connectionId);
        try {
            return consumptionRecordMapper.mapToCIM(consumptionRecord);
        } catch (InvalidMappingException e) {
            // TODO In the future this should also inform the administrative console about the invalid mapping
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
        List<AtPermissionRequest> permissionRequests = permissionRequestService.findByMeteringPointIdAndDate(
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