package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.persistence.DkEnerginetCustomerPermissionRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PermissionRequestService {
    private final PermissionRequestFactory requestFactory;
    private final DkEnerginetCustomerPermissionRequestRepository repository;

    public PermissionRequestService(
            PermissionRequestFactory requestFactory,
            DkEnerginetCustomerPermissionRequestRepository repository
    ) {
        this.requestFactory = requestFactory;
        this.repository = repository;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId).map(request ->
                new ConnectionStatusMessage(
                        request.connectionId(),
                        request.permissionId(),
                        request.dataNeedId(),
                        request.dataSourceInformation(),
                        request.status())

        );
    }

    public Optional<DkEnerginetCustomerPermissionRequest> findByPermissionId(String permissionId) {
        return repository.findByPermissionId(permissionId).map(requestFactory::create);
    }

    public List<DkEnerginetCustomerPermissionRequest> findAllAcceptedPermissionRequests() {
        return repository.findAllByStatusIs(PermissionProcessStatus.ACCEPTED)
                .stream()
                .map(requestFactory::create)
                .filter(Objects::nonNull)
                .toList();
    }
}