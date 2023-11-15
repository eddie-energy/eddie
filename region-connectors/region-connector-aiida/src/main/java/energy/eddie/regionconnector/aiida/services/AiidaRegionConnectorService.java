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

    @Autowired
    public AiidaRegionConnectorService(AiidaFactory aiidaFactory, AiidaPermissionRequestRepository repository) {
        this.aiidaFactory = aiidaFactory;
        this.repository = repository;
    }

    public Publisher<ConnectionStatusMessage> connectionStatusMessageFlux() {
        return statusMessageSink.asFlux();
    }

    @Override
    public void close() {
        statusMessageSink.tryEmitComplete();
    }

    /**
     * Creates a new {@link energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest} and persists it
     * to be compliant with the process model.
     * Returns the necessary information which AIIDA needs to set up a permission with this EP's service.
     * This information is intended to be encoded in a QR code and be displayed to the customer.
     *
     * @param creationRequest Request from the frontend containing necessary information for creating a new permission.
     * @return Necessary data that should be displayed on the frontend.
     */
    public PermissionDto createNewPermission(PermissionRequestForCreation creationRequest) throws FutureStateException, PastStateException {
        var permissionRequest = aiidaFactory.createPermissionRequest(creationRequest.connectionId(),
                creationRequest.dataNeedId(), creationRequest.startTime(), creationRequest.expirationTime(), this);

        LOGGER.info("Created a new permission request with permissionId {} for connectionId {}", permissionRequest.permissionId(),
                permissionRequest.connectionId());

        // TODO need to save in db before these calls or after each one or is it sufficient to save after all
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();

        repository.save(permissionRequest);

        return aiidaFactory.createPermissionDto(permissionRequest);
    }

    // TODO is this intended like this, that createNewPermission calls a method on the aiidaRequest, which in turn calls this method
    // and just for this, the permissionrequests need to get passed a reference to this service.
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
