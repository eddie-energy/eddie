// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation;

import energy.eddie.api.agnostic.DataSourceInformation;

public class SimulationDataSourceInformation implements DataSourceInformation {
    @Override
    public String countryCode() {
        return "DE";
    }

    @Override
    public String regionConnectorId() {
        return "sim";
    }

    @Override
    public String meteredDataAdministratorId() {
        return "sim";
    }

    @Override
    public String permissionAdministratorId() {
        return "sim";
    }
}
