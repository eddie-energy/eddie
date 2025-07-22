package energy.eddie.core.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import energy.eddie.api.agnostic.DataSourceInformation;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SimpleDataSourceInformation(String countryCode,
                                          String regionConnectorId,
                                          String meteredDataAdministratorId,
                                          String permissionAdministratorId) implements DataSourceInformation {}
