// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.permission;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata;

public class FingridDataSourceInformation implements DataSourceInformation {

    private static final RegionConnectorMetadata REGION_CONNECTOR_METADATA = FingridRegionConnectorMetadata.INSTANCE;

    @Override
    public String countryCode() {
        return REGION_CONNECTOR_METADATA.countryCode();
    }

    @Override
    public String regionConnectorId() {
        return REGION_CONNECTOR_METADATA.id();
    }

    @Override
    public String meteredDataAdministratorId() {
        return "Fingrid";
    }

    @Override
    public String permissionAdministratorId() {
        return "Fingrid";
    }
}
