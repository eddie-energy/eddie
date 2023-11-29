package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final EsPermissionRequestRepository repository;

    @Autowired
    public PermissionRequestService(EsPermissionRequestRepository repository) {
        this.repository = repository;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return Optional.empty();
    }

    public void acceptPermission(String permissionId) throws StateTransitionException, PermissionNotFoundException {
        var request = repository.findByPermissionId(permissionId);

        if (request.isEmpty()) {
            throw new PermissionNotFoundException(permissionId);
        }

        var permissionRequest = request.get();
        permissionRequest.accept();

        // TODO is this a blocking request that will prevent the controller from returning?
        //datadisScheduler.pullAvailableHistoricalData(permissionRequest);
    }

    public void rejectPermission(String permissionId) throws PermissionNotFoundException, StateTransitionException {
        var request = repository.findByPermissionId(permissionId);

        if (request.isEmpty()) {
            throw new PermissionNotFoundException(permissionId);
        }

        request.get().reject();
    }
}
