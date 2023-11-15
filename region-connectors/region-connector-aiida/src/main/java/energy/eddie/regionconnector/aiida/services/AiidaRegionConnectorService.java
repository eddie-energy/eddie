package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.aiida.AiidaFactory;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequestRepository;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

@Service
public class AiidaRegionConnectorService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorService.class);
    private final Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().multicast().onBackpressureBuffer();
    private final AiidaFactory aiidaFactory;
    private final AiidaPermissionRequestRepository repository;

    /**
     * Creates a new {@link energy.eddie.regionconnector.aiida.AiidaRegionConnector} that can be used to request
     * permissions for near real-time data.
     *
     * @param aiidaFactory Factory used to construct {@link AiidaPermissionRequest}s and {@link PermissionDto}s.
     * @param repository   Repository to be used for persisting permission requests.
     */
    @Autowired
    public AiidaRegionConnectorService(AiidaFactory aiidaFactory, AiidaPermissionRequestRepository repository) {
        this.aiidaFactory = aiidaFactory;
        this.repository = repository;
    }

    /**
     * Returns the Flux where all {@link ConnectionStatusMessage}s will be published.
     *
     * @return Flux of status messages
     */
    public Publisher<ConnectionStatusMessage> connectionStatusMessageFlux() {
        return statusMessageSink.asFlux();
    }

    /**
     * Closes the service and emits a complete signal on the connection status message Flux.
     */
    @Override
    public void close() {
        statusMessageSink.tryEmitComplete();
    }

    /**
     * Creates a new {@link energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest} and initiates the
     * permission process (i.e. validates and sends to PA).
     * Returns the necessary information which AIIDA needs to set up a permission with this EP's service.
     * This information is intended to be encoded in a QR code and be displayed to the customer.
     *
     * @param creationRequest Request from the frontend containing necessary information for creating a new permission.
     * @return Necessary data that should be displayed on the frontend.
     */
    public PermissionDto createNewPermission(PermissionRequestForCreation creationRequest) throws FutureStateException, PastStateException {
        var permissionRequest = aiidaFactory.createPermissionRequest(creationRequest.connectionId(),
                creationRequest.dataNeedId(), creationRequest.startTime(), creationRequest.expirationTime(), this);

        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        repository.save(permissionRequest);

        LOGGER.info("Created, validated, sent to PA and persisted a new permission request with permissionId {} for connectionId {}", permissionRequest.permissionId(),
                permissionRequest.connectionId());

        return aiidaFactory.createPermissionDto(permissionRequest);
    }

    /**
     * Publishes a {@link ConnectionStatusMessage} with status {@link PermissionProcessStatus#SENT_TO_PERMISSION_ADMINISTRATOR},
     * as the actual sending to permission manager is done by the REST API by returning the response for the permission request.
     *
     * @param permissionRequest Request for which the status message should be sent
     */
    public void sendToPermissionAdministrator(AiidaPermissionRequest permissionRequest) {
        String connectionId = permissionRequest.connectionId();
        var statusMessage = new ConnectionStatusMessage(connectionId, permissionRequest.permissionId(),
                permissionRequest.dataNeedId(), PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        var result = statusMessageSink.tryEmitNext(statusMessage);

        if (result.isFailure()) {
            LOGGER.error("Error while emitting ConnectionStatusMessage for new permission with connectionId {}. Error was {}",
                    connectionId, result);
        }
    }
}
