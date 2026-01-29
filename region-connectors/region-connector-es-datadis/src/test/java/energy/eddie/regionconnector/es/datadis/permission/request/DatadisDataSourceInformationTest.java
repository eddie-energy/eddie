// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatadisDataSourceInformationTest {

    @Test
    void countryCode() {
        var datadisdataSourceInformation = new DatadisDataSourceInformation(mock(EsPermissionRequest.class));

        var countryCode = datadisdataSourceInformation.countryCode();

        assertEquals("ES", countryCode);
    }

    @Test
    void permissionAdministratorId() {
        var datadisdataSourceInformation = new DatadisDataSourceInformation(mock(EsPermissionRequest.class));

        var permissionAdministratorId = datadisdataSourceInformation.permissionAdministratorId();

        assertEquals("Datadis", permissionAdministratorId);
    }

    @Test
    void meteredDataAdministratorId() {
        var permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.distributorCode()).thenReturn(Optional.of(DistributorCode.VIESGO));
        var datadisdataSourceInformation = new DatadisDataSourceInformation(permissionRequest);

        var meteredDataAdministratorId = datadisdataSourceInformation.meteredDataAdministratorId();

        assertEquals(DistributorCode.VIESGO.toString(), meteredDataAdministratorId);
    }

    @Test
    void regionConnectorId() {
        var datadisdataSourceInformation = new DatadisDataSourceInformation(mock(EsPermissionRequest.class));

        var regionConnectorId = datadisdataSourceInformation.regionConnectorId();

        assertEquals("es-datadis", regionConnectorId);
    }
}