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
    public PermissionEventRepository getPermissionEventRepositoryByCountryCode(String countryCode) {
        String rcBeanName = RegionConnectorRepository.fromCountryCode(countryCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid region connector country: " + countryCode))
                .getBeanName();
        PermissionEventRepository repository = permissionEventRepositories.get(rcBeanName);

        if (repository == null) {
            throw new IllegalArgumentException("No repository found for bean name: " + rcBeanName);
        }

        return repository;
    }
}
