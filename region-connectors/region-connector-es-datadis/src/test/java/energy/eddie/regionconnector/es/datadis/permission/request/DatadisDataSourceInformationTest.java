package energy.eddie.regionconnector.es.datadis.permission.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatadisDataSourceInformationTest {

    @Test
    void countryCode() {
        var datadisdataSourceInformation = new DatadisDataSourceInformation();

        var countryCode = datadisdataSourceInformation.countryCode();

        assertEquals("ES", countryCode);
    }

    @Test
    void permissionAdministratorId() {
        var datadisdataSourceInformation = new DatadisDataSourceInformation();

        var permissionAdministratorId = datadisdataSourceInformation.permissionAdministratorId();

        assertEquals("Datadis", permissionAdministratorId);
    }

    @Test
    void meteredDataAdministratorId() {
        var datadisdataSourceInformation = new DatadisDataSourceInformation();

        var meteredDataAdministratorId = datadisdataSourceInformation.meteredDataAdministratorId();

        assertEquals("Not available", meteredDataAdministratorId);
    }

    @Test
    void regionConnectorId() {
        var datadisdataSourceInformation = new DatadisDataSourceInformation();

        var regionConnectorId = datadisdataSourceInformation.regionConnectorId();

        assertEquals("es-datadis", regionConnectorId);
    }
}