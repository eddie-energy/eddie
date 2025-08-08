package energy.eddie.core.services;

import energy.eddie.api.agnostic.outbound.PermissionEventRepositories;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PermissionEventService implements PermissionEventRepositories {

    private final Map<String, PermissionEventRepository> permissionEventRepositories = new HashMap<>();

    public void registerPermissionEventRepository(PermissionEventRepository repository, String regionConnectorId) {
        permissionEventRepositories.put(regionConnectorId, repository);
    }

    @Override
    public PermissionEventRepository getPermissionEventRepositoryByRegionConnectorId(String regionConnectorId) {
        PermissionEventRepository repository = permissionEventRepositories.get(regionConnectorId);

        if (repository == null) {
            throw new IllegalArgumentException("No repository found for region connector: " + regionConnectorId);
        }

        return repository;
    }
}
