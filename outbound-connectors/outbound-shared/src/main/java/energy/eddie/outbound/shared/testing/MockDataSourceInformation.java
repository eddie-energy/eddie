package energy.eddie.outbound.shared.testing;

import energy.eddie.api.agnostic.DataSourceInformation;

public record MockDataSourceInformation(String countryCode,
                                        String regionConnectorId,
                                        String permissionAdministratorId,
                                        String meteredDataAdministratorId) implements DataSourceInformation {
}
