package energy.eddie.regionconnector.es.datadis.permission.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatadisRegionalInformationTest {

    @Test
    void countryCode() {
        var datadisRegionalInformation = new DatadisRegionalInformation();

        var countryCode = datadisRegionalInformation.countryCode();

        assertEquals("es", countryCode);
    }

    @Test
    void permissionAdministratorId() {
        var datadisRegionalInformation = new DatadisRegionalInformation();

        var permissionAdministratorId = datadisRegionalInformation.permissionAdministratorId();

        assertEquals("Datadis", permissionAdministratorId);
    }

    @Test
    void meteringDataAdministratorId() {
        var datadisRegionalInformation = new DatadisRegionalInformation();

        var meteringDataAdministratorId = datadisRegionalInformation.meteringDataAdministratorId();

        assertEquals("Not available", meteringDataAdministratorId);
    }

    @Test
    void regionConnectorId() {
        var datadisRegionalInformation = new DatadisRegionalInformation();

        var regionConnectorId = datadisRegionalInformation.regionConnectorId();

        assertEquals("es-datadis", regionConnectorId);
    }
}