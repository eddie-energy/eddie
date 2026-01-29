// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class DatadisDataSourceInformation implements DataSourceInformation {
    private static final DatadisRegionConnectorMetadata regionConnectorMetadata = DatadisRegionConnectorMetadata.getInstance();
    private final EsPermissionRequest permissionRequest;

    public DatadisDataSourceInformation(EsPermissionRequest permissionRequest) {
        this.permissionRequest = permissionRequest;
    }

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
        return permissionRequest.distributorCode()
                                .map(DistributorCode::toString)
                                .orElse("Not available");
    }

    @Override
    public String permissionAdministratorId() {
        return "Datadis";
    }
}
