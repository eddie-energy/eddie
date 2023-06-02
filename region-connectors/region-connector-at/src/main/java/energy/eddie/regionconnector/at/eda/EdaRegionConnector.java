package energy.eddie.regionconnector.at.eda;

import energy.eddie.regionconnector.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.api.v0.models.ConsumptionRecord;
import energy.eddie.regionconnector.at.api.RegionConnectorAT;
import energy.eddie.regionconnector.at.api.SendCCMORequestResult;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import static java.util.Objects.requireNonNull;

public class EdaRegionConnector implements RegionConnectorAT {

    private final AtConfiguration atConfiguration;
    private final EdaAdapter edaAdapter;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final EdaIdMapper edaIdMapper;

    private final Logger logger = LoggerFactory.getLogger(EdaRegionConnector.class);

    private final SubmissionPublisher<ConnectionStatusMessage> permissionStatusPublisher = new SubmissionPublisher<>();
    private final SubmissionPublisher<ConsumptionRecord> consumptionRecordSubmissionPublisher = new SubmissionPublisher<>();

    public EdaRegionConnector(AtConfiguration atConfiguration, EdaAdapter edaAdapter, EdaIdMapper edaIdMapper) throws TransmissionException {
        requireNonNull(atConfiguration);
        requireNonNull(edaAdapter);
        requireNonNull(edaIdMapper);

        this.atConfiguration = atConfiguration;
        this.edaAdapter = edaAdapter;
        this.consumptionRecordMapper = new ConsumptionRecordMapper(atConfiguration.timeZone());
        this.edaIdMapper = edaIdMapper;

        edaAdapter.getCMRequestStatusStream().subscribe(this::processCMRequestStatus);

        edaAdapter.getConsumptionRecordStream().subscribe(this::processConsumptionRecords);

        edaAdapter.start();
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> consumptionRecordStream() {
        return consumptionRecordSubmissionPublisher;
    }

    @Override
    public void revokePermission(String permissionId) {
        throw new UnsupportedOperationException("Revoke permission is not yet implemented");
    }

    @Override
    public SendCCMORequestResult sendCCMORequest(String connectionId, CCMORequest request) throws TransmissionException, InvalidDsoIdException, JAXBException {
        requireNonNull(connectionId);
        requireNonNull(request);
        var cmRequest = request.toCMRequest();
        var permissionId = UUID.randomUUID().toString();
        edaIdMapper.addMappingInfo(cmRequest.getProcessDirectory().getConversationId(), cmRequest.getProcessDirectory().getCMRequestId(), new MappingInfo(permissionId, connectionId));

        edaAdapter.sendCMRequest(cmRequest);

        return new SendCCMORequestResult(permissionId, cmRequest.getProcessDirectory().getCMRequestId());
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> connnectionStatusMessageStream() {
        return permissionStatusPublisher;
    }

    /**
     * Process an CMRequestStatus, convert it to a ConnectionStatusMessage and add connectionId and permissionId for identification before submitting it to the connection status message publisher
     *
     * @param cmRequestStatus the CMRequestStatus to process
     */
    private void processCMRequestStatus(CMRequestStatus cmRequestStatus) {
        var mappingInfo = edaIdMapper.getMappingInfoForConversationIdOrRequestID(cmRequestStatus.getConversationId(), cmRequestStatus.getCMRequestId().orElse(null));
        if (mappingInfo.isEmpty()) {
            // should not happen if a persistent mapping is used
            // TODO inform the administrative console if it happens
            logger.warn("Received CMRequestStatus for unknown conversationId or requestId: {}", cmRequestStatus);
            return;
        }

        var permissionId = mappingInfo.get().permissionId();
        var connectionId = mappingInfo.get().connectionId();

        var message = cmRequestStatus.getMessage();
        var now = ZonedDateTime.now(atConfiguration.timeZone());

        var status = switch (cmRequestStatus.getStatus()) {
            case ACCEPTED -> ConnectionStatusMessage.Status.GRANTED;
            case ERROR -> ConnectionStatusMessage.Status.ERROR;
            case REJECTED -> ConnectionStatusMessage.Status.REJECTED;
            case SENT, RECEIVED, DELIVERED -> ConnectionStatusMessage.Status.REQUESTED;
        };

        permissionStatusPublisher.submit(new ConnectionStatusMessage(connectionId, permissionId, now, status, message));
    }

    /**
     * Process an EDA consumption record, convert it to a CIM consumption record and add connectionId and permissionId for identification before submitting it to the consumption record publisher
     *
     * @param consumptionRecord the consumption record to process
     */
    private void processConsumptionRecords(at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.ConsumptionRecord consumptionRecord) {
        // this should only return null, if the consumption record was delivered in a separate EBUtilities process from the CCMO request i.e. when we request MeteringData (data from the future) and not HistoricalMeteringData
        // if we request MeteringData, we will receive a consumption record with a conversationId that we don't know
        // in this case the only way for us to identify which permission this consumption record belongs to is by checking if we have an ongoing permission for the metering point consumption
        // which also only works if we only have none overlapping permission periods per metering point
        // a metering point might also belong to multiple connection ids, so this is also not unique enough
        // i.e. future TODO rework mapper to support MeteringData
        var mappingInfo = edaIdMapper.getMappingInfoForConversationIdOrRequestID(consumptionRecord.getProcessDirectory().getConversationId(), null);
        var permissionId = mappingInfo.map(MappingInfo::permissionId).orElse(null);
        var connectionId = mappingInfo.map(MappingInfo::connectionId).orElse(null);

        try {
            ConsumptionRecord cimConsumptionRecord = consumptionRecordMapper.mapToCIM(consumptionRecord, permissionId, connectionId);
            consumptionRecordSubmissionPublisher.submit(cimConsumptionRecord);
        } catch (InvalidMappingException e) {
            // TODO In the future this should also inform the administrative console about the invalid mapping
            logger.error("Could not map consumption record to CIM consumption record", e);
        }
    }
}
