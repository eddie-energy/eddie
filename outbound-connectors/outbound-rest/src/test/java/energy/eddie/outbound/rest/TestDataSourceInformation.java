package energy.eddie.outbound.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import energy.eddie.api.agnostic.DataSourceInformation;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TestDataSourceInformation(String countryCode,
                                        String regionConnectorId,
                                        String meteredDataAdministratorId,
                                        String permissionAdministratorId) implements DataSourceInformation {}
