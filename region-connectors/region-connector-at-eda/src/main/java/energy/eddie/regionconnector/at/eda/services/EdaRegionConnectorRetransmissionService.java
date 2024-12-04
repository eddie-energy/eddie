package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import energy.eddie.regionconnector.at.eda.requests.CPRequestResult;
import energy.eddie.regionconnector.at.eda.requests.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Service
public class EdaRegionConnectorRetransmissionService implements RegionConnectorRetransmissionService, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRegionConnectorRetransmissionService.class);

    private final AtConfiguration configuration;
    private final EdaAdapter edaAdapter;
    private final AtPermissionRequestRepository repository;

    /**
     * Map of message ids to sinks for retransmission results
     * Since we return a Mono to the caller, we can't recover from a shutdown as we can not persist Sinks.
     * To handle this, when the service is shutting down, we will emit a {@link Failure} response to all currently stored sinks.
     * Since this request can be considered idempotent, the caller can just retry the request if they do not receive the requested data.
     */
    private final ConcurrentHashMap<String, Pair<Sinks.One<RetransmissionResult>, String>> retransmissionResults = new ConcurrentHashMap<>();

    public EdaRegionConnectorRetransmissionService(
            AtConfiguration configuration,
            EdaAdapter edaAdapter,
            AtPermissionRequestRepository repository
    ) {
        this.configuration = configuration;
        this.edaAdapter = edaAdapter;
        this.repository = repository;

        edaAdapter.getCPRequestResultStream().subscribe(this::processCpRequestResults);
    }

    @Override
    public Mono<RetransmissionResult> requestRetransmission(RetransmissionRequest retransmissionRequest) {
        var permissionRequest = repository.findByPermissionId(retransmissionRequest.permissionId());
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);

        if (permissionRequest.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", retransmissionRequest.permissionId());
            return Mono.just(new PermissionRequestNotFound(
                    retransmissionRequest.permissionId(),
                    now
            ));
        }

        var request = permissionRequest.get();

        if (request.status() != PermissionProcessStatus.ACCEPTED && request.status() != PermissionProcessStatus.FULFILLED) {
            LOGGER.warn("Can only request retransmission for accepted or fulfilled permissions, current status: {}",
                        request.status());
            return Mono.just(new NoActivePermission(
                    retransmissionRequest.permissionId(),
                    now
            ));
        }

        if (request.granularity() == null) {
            LOGGER.warn("Retransmission of MasterData not supported");
            return Mono.just(new NotSupported(
                    retransmissionRequest.permissionId(),
                    now,
                    "Retransmission of MasterData not supported"
            ));
        }

        if (retransmissionRequest.from().isBefore(request.start()) ||
            retransmissionRequest.to().isAfter(request.end())
        ) {
            LOGGER.warn("Retransmission request not within permission time frame");
            return Mono.just(new NoPermissionForTimeFrame(
                    retransmissionRequest.permissionId(),
                    now
            ));
        }

        ZonedDateTime created = ZonedDateTime.now(AT_ZONE_ID);
        var messageId = new MessageId(configuration.eligiblePartyId(), created).toString();

        while (retransmissionResults.containsKey(messageId)) {
            // MessageId is only accurate to milliseconds, ensure uniqueness
            created = created.plus(1, ChronoUnit.MILLIS);
            messageId = new MessageId(configuration.eligiblePartyId(), created).toString();
        }

        CPRequestCR cpRequestCR = new CPRequestCR(
                request.dataSourceInformation().permissionAdministratorId(),
                request.meteringPointId().orElseThrow(),
                messageId,
                retransmissionRequest.from(),
                retransmissionRequest.to(),
                request.granularity(),
                configuration
        );

        Sinks.One<RetransmissionResult> sink = Sinks.one();
        retransmissionResults.put(messageId, new Pair<>(sink, retransmissionRequest.permissionId()));

        try {
            edaAdapter.sendCPRequest(cpRequestCR);
            return sink.asMono();
        } catch (TransmissionException e) {
            retransmissionResults.remove(messageId);
            LOGGER.error("Error sending CPRequest: {}", cpRequestCR, e);
            return Mono.just(new Failure(
                    retransmissionRequest.permissionId(),
                    now,
                    "Could not send request, investigate logs"
            ));
        }
    }

    @Override
    public void close() throws Exception {
        for (var pair : retransmissionResults.values()) {
            var sink = pair.key();
            var permissionId = pair.value();
            var result = sink.tryEmitValue(
                    new Failure(permissionId,
                                ZonedDateTime.now(AT_ZONE_ID),
                                "Service is shutting down, unclear if request got through to the MDA")
            );
            if (result.isFailure()) {
                LOGGER.atTrace().log("Could not emit value to sink while shutting down: result = {}", result);
            }
        }
        retransmissionResults.clear();
    }

    private void processCpRequestResults(CPRequestResult cpRequestResult) {
        var pair = retransmissionResults.remove(cpRequestResult.messageId());
        if (pair == null) {
            LOGGER.warn("No sink found for message id: {}", cpRequestResult.messageId());
            return;
        }
        var sink = pair.key();
        var permissionId = pair.value();
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);

        RetransmissionResult result = switch (cpRequestResult.result()) {
            case ACCEPTED -> new Success(permissionId, now);
            case NO_DATA_AVAILABLE -> new DataNotAvailable(permissionId, now);
            case PONTON_ERROR -> new Failure(permissionId, now, "Could not transmit the request to the dso");
            // The cases below should essentially not happen.
            // Unknown response code indicates an update in the CR_REQ_PT process that we are not aware of.
            case UNKNOWN_RESPONSE_CODE_ERROR ->
                    new Failure(permissionId, now, "Got an unknown response code from the dso");
            // This would indicate an invalid permission request state as we only request retransmissions for accepted or fulfilled permissions
            case METERING_POINT_NOT_FOUND ->
                    new Failure(permissionId, now, "Metering point associated with the permission request not found");
            // This indicates that the metering point is not assigned at the dso, we cant do anything about this
            case METERING_POINT_NOT_ASSIGNED -> new Failure(
                    permissionId,
                    now,
                    "Metering point associated with the permission request not assigned"
            );
            // This indicates that we sent an invalid process date to the dso, this should not happen
            case PROCESS_DATE_INVALID -> new Failure(permissionId, now, "Process date invalid");
        };

        sink.emitValue(result, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
    }
}
