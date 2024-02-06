package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionRequestService {
    private final DkEnerginetCustomerPermissionRequestRepository repository;
    private final PermissionRequestFactory requestFactory;

    public PermissionRequestService(
            DkEnerginetCustomerPermissionRequestRepository repository,
            PermissionRequestFactory requestFactory
    ) {
        this.repository = repository;
        this.requestFactory = requestFactory;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId).map(request ->
                new ConnectionStatusMessage(
                        request.connectionId(),
                        request.permissionId(),
                        request.dataNeedId(),
                        request.dataSourceInformation(),
                        request.state().status())
        );
    }

    public Optional<DkEnerginetCustomerPermissionRequest> findByPermissionId(String permissionId) {
        var permissionRequest = repository.findByPermissionId(permissionId);
        return permissionRequest
                .map(requestFactory::create);
    }

    public List<DkEnerginetCustomerPermissionRequest> findAllAcceptedPermissionRequests() {
        return repository.findAll()
                .stream()
                .filter(req -> req.state().status() == PermissionProcessStatus.ACCEPTED)
                .map(requestFactory::create)
                .toList();
    }
}