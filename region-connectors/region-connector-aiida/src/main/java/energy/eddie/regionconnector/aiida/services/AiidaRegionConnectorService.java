package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.AiidaFactory;
import energy.eddie.regionconnector.aiida.AiidaRegionConnector;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.concurrent.Flow;

@Service
public class AiidaRegionConnectorService implements Mvp1ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorService.class);
    private final Sinks.Many<ConnectionStatusMessage> statusMessageSink;
    private final AiidaFactory aiidaFactory;
    private final AiidaPermissionRequestRepository repository;
    private final Sinks.Many<TerminationRequest> terminationRequestSink;

    /**
     * Creates a new {@link AiidaRegionConnector} that can be used to request permissions for near real-time data.
     *
     * @param statusMessageSink      Sink to which all {@link ConnectionStatusMessage}s should be published.
     * @param aiidaFactory           Factory used to construct {@link AiidaPermissionRequest}s and {@link PermissionDto}s.
     * @param repository             Repository to be used for querying permission requests.
     * @param terminationRequestSink Flux on which any termination requests that should be sent via Kafka to a specific topic are published.
     */
    @Autowired
    public AiidaRegionConnectorService(
            AiidaFactory aiidaFactory,
            AiidaPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> statusMessageSink,
            Sinks.Many<TerminationRequest> terminationRequestSink) {
        this.statusMessageSink = statusMessageSink;
        this.aiidaFactory = aiidaFactory;
        this.repository = repository;
        this.terminationRequestSink = terminationRequestSink;
    }

    /**
     * Returns the Flow where all {@link ConnectionStatusMessage}s will be published.
     *
     * @return Flow of status messages.
     */
    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(statusMessageSink.asFlux());
    }

    /**
     * Closes the service and emits a complete signal on the connection status message and the termination request Flux.
     */
    @Override
    public void close() {
        statusMessageSink.tryEmitComplete();
        terminationRequestSink.tryEmitComplete();
    }

    /**
     * Creates a new {@link AiidaPermissionRequest} and initiates the permission process (i.e. validates and sends to PA).
     * Returns the necessary information which AIIDA needs to set up a permission with this EP's service.
     * This information is intended to be encoded in a QR code and be displayed to the customer.
     *
     * @param creationRequest Request containing necessary information for creating a new permission.
     * @return Necessary data that should be displayed to the customer.
     */
    public PermissionDto createNewPermission(PermissionRequestForCreation creationRequest) throws StateTransitionException, DataNeedNotFoundException {
        var permissionRequest = aiidaFactory.createPermissionRequest(creationRequest.connectionId(),
                creationRequest.dataNeedId());

        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Created, validated and sent to PA a new permission request with permissionId {} for connectionId {}",
                    permissionRequest.permissionId(), permissionRequest.connectionId());

        return aiidaFactory.createPermissionDto(permissionRequest);
    }

    /**
     * Returns the {@link AiidaPermissionRequest} from the persistence layer on which state transitions may be executed,
     * as it is ensured, that any {@link energy.eddie.regionconnector.shared.permission.requests.extensions.Extension}s are executed.
     *
     * @param permissionId ID of the permission to return.
     * @return Permission request of the specified ID.
     * @throws PermissionNotFoundException If no permission with the ID is saved in the persistence layer.
     */
    public AiidaPermissionRequestInterface getPermissionRequestById(String permissionId) throws PermissionNotFoundException {
        var request = repository.findByPermissionId(permissionId).orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return aiidaFactory.recreatePermissionRequest(request);
    }

    /**
     * Sends a termination request to the AIIDA instance associated with {@code permissionId} and updates the state
     * of the associated {@link PermissionRequest}.
     * An error will be logged if the specified permissionId does not exist.
     *
     * @param permissionId ID of the permission that should be terminated.
     * @throws StateTransitionException Thrown when a state could not successfully be transitioned.
     */
    public void terminatePermission(String permissionId) throws StateTransitionException {
        LOGGER.info("Got request to terminate permission {}", permissionId);
        Optional<AiidaPermissionRequestInterface> optional = repository.findByPermissionId(permissionId);

        if (optional.isEmpty()) {
            LOGGER.error("Was requested to terminate permission {}, but could not find a matching permission in the repository", permissionId);
            return;
        }

        var request = aiidaFactory.recreatePermissionRequest(optional.get());
        terminationRequestSink.tryEmitNext(new TerminationRequest(request.connectionId(), request.terminationTopic()));

        request.terminate();
    }
}