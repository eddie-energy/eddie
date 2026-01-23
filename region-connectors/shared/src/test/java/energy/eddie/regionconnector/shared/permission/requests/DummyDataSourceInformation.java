// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.permission.requests;

import energy.eddie.api.agnostic.DataSourceInformation;

public class DummyDataSourceInformation implements DataSourceInformation {
    @Override
    public String countryCode() {
        return "AT";
    }

    @Override
    public String regionConnectorId() {
        return "dummy-rc";
    }

    @Override
    public String meteredDataAdministratorId() {
        return null;
    }

    @Override
    public String permissionAdministratorId() {
        return null;
    }
}
