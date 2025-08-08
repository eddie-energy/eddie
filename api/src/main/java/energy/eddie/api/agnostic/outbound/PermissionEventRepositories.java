package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;

public interface PermissionEventRepositories {
    PermissionEventRepository getPermissionEventRepositoryByCountryCode(String countryCode);
}
