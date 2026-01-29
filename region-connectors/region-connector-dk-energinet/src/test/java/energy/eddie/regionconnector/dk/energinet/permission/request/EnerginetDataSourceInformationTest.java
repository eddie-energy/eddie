// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnerginetDataSourceInformationTest {

    @Test
    void countryCode() {
        var energinetdataSourceInformation = new EnerginetDataSourceInformation();

        var countryCode = energinetdataSourceInformation.countryCode();

        assertEquals("DK", countryCode);
    }

    @Test
    void regionConnectorId() {
        var energinetdataSourceInformation = new EnerginetDataSourceInformation();

        var regionConnectorId = energinetdataSourceInformation.regionConnectorId();

        assertEquals("dk-energinet", regionConnectorId);
    }

    @Test
    void permissionAdministratorId() {
        var energinetdataSourceInformation = new EnerginetDataSourceInformation();

        var permissionAdministratorId = energinetdataSourceInformation.permissionAdministratorId();

        assertEquals("Energinet", permissionAdministratorId);
    }

    @Test
    void meteredDataAdministratorId() {
        var energinetdataSourceInformation = new EnerginetDataSourceInformation();

        var meteredDataAdministratorId = energinetdataSourceInformation.meteredDataAdministratorId();

        assertEquals("Energinet", meteredDataAdministratorId);
    }
}