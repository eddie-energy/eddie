package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class PermissionRequestService {
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

        var request = permissionRequestFactory.create(requestForCreation);
        request.validate();
        request.sendToPermissionAdministrator();
        return request;
    }

    public void terminatePermission(String permissionId) throws PermissionNotFoundException, StateTransitionException {
        var permissionRequest = getPermissionRequestById(permissionId);

        permissionRequest.terminate();
    }
}
