package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;

import java.util.Optional;

public interface PermissionEventRepositories {
    Optional<PermissionEventRepository> getPermissionEventRepositoryByRegionConnectorId(String regionConnectorId);
}
