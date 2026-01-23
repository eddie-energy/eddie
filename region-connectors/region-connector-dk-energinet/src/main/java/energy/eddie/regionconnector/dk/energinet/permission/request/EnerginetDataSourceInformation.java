// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;

public class EnerginetDataSourceInformation implements DataSourceInformation {
    private static final EnerginetRegionConnectorMetadata regionConnectorMetadata = EnerginetRegionConnectorMetadata.getInstance();

    private static final String ENERGINET = "Energinet";

    @Override
    public String countryCode() {
        return regionConnectorMetadata.countryCode();
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }

    @Override
    public String meteredDataAdministratorId() {
        return ENERGINET;
    }

    @Override
    public String permissionAdministratorId() {
        return ENERGINET;
    }
}
