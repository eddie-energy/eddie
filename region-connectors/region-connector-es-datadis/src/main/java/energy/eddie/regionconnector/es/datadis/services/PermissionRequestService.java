package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class PermissionRequestService implements AuthorizationResponseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final EsPermissionRequestRepository repository;
    private final PermissionRequestFactory permissionRequestFactory;
    private final DatadisScheduler datadisScheduler;

    @Autowired
    public PermissionRequestService(
            EsPermissionRequestRepository repository,
            PermissionRequestFactory permissionRequestFactory,
            DatadisScheduler datadisScheduler) {
        this.repository = repository;
        this.permissionRequestFactory = permissionRequestFactory;
        this.datadisScheduler = datadisScheduler;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId)
                .map(permissionRequest -> new ConnectionStatusMessage(
                                permissionRequest.connectionId(),
                                permissionRequest.permissionId(),
                                permissionRequest.dataNeedId(),
                                permissionRequest.regionalInformation(),
                                permissionRequest.state().status()
                        )
                );
    }

    public void acceptPermission(String permissionId) throws StateTransitionException, PermissionNotFoundException {
        LOGGER.info("Got request to accept permission {}", permissionId);

        var permissionRequest = getPermissionRequestById(permissionId);
        permissionRequest.accept();

        // TODO is this a blocking request that will prevent the controller from returning?
        datadisScheduler.pullAvailableHistoricalData(permissionRequest);
    }

    public void rejectPermission(String permissionId) throws PermissionNotFoundException, StateTransitionException {
        LOGGER.info("Got request to reject permission {}", permissionId);

        var permissionRequest = getPermissionRequestById(permissionId);
        permissionRequest.reject();
    }

    private EsPermissionRequest getPermissionRequestById(String permissionId) throws PermissionNotFoundException {
        return repository.findByPermissionId(permissionId).orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    public PermissionRequest createAndSendPermissionRequest(PermissionRequestForCreation requestForCreation) throws StateTransitionException {
        LOGGER.info("Got request to create a new permission, request was: {}", requestForCreation);

        var request = permissionRequestFactory.create(requestForCreation, this);
        request.validate();
        request.sendToPermissionAdministrator();
        return request;
    }

    public void terminatePermission(String permissionId) throws PermissionNotFoundException, StateTransitionException {
        var permissionRequest = getPermissionRequestById(permissionId);

        permissionRequest.terminate();
    }

    @Override
    public void handleAuthorizationRequestResponse(String permissionId, AuthorizationRequestResponse response) {
        var optional = repository.findByPermissionId(permissionId);
        if (optional.isEmpty()) {
            LOGGER.error("Received authorization response {} for unknown permission request {}", response, permissionId);
            return;
        }

        var permissionRequest = optional.get();
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
