package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PermissionRequestService {
    private final DkPermissionRequestRepository repository;

    public PermissionRequestService(
            DkPermissionRequestRepository repository
    ) {
        this.repository = repository;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId).map(request ->
                                                                       new ConnectionStatusMessage(
                                                                               request.connectionId(),
                                                                               request.permissionId(),
                                                                               request.dataNeedId(),
                                                                               request.dataSourceInformation(),
                                                                               request.status(),
                                                                               request.errors())

        );
    }
}
