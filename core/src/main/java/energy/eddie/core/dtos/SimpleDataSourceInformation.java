// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import energy.eddie.api.agnostic.DataSourceInformation;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SimpleDataSourceInformation(String countryCode,
                                          String regionConnectorId,
                                          String meteredDataAdministratorId,
                                          String permissionAdministratorId) implements DataSourceInformation {}
