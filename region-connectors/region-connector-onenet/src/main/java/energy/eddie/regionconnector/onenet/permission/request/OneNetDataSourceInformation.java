// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.permission.request;

import energy.eddie.cim.agnostic.DataSourceInformation;

import static energy.eddie.regionconnector.onenet.OneNetRegionConnectorMetadata.REGION_CONNECTOR_ID;

public class OneNetDataSourceInformation implements DataSourceInformation {
    @Override
    public String countryCode() {
        // TODO: Replace with actual country code
        return "it";
    }

    @Override
    public String regionConnectorId() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String meteredDataAdministratorId() {
        // TODO: Replace with actual metered data administrator ID
        return "onenet";
    }

    @Override
    public String permissionAdministratorId() {
        // TODO: Replace with actual permission administrator ID
        return "onenet";
    }
}
