package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnerginetRegionalInformationTest {

    @Test
    void countryCode() {
        var energinetRegionalInformation = new EnerginetRegionalInformation();

        var countryCode = energinetRegionalInformation.countryCode();

        assertEquals("dk", countryCode);
    }

    @Test
    void regionConnectorId() {
        var energinetRegionalInformation = new EnerginetRegionalInformation();

        var regionConnectorId = energinetRegionalInformation.regionConnectorId();

        assertEquals("dk-energinet", regionConnectorId);
    }

    @Test
    void permissionAdministratorId() {
        var energinetRegionalInformation = new EnerginetRegionalInformation();

        var permissionAdministratorId = energinetRegionalInformation.permissionAdministratorId();

        assertEquals("Energinet", permissionAdministratorId);
    }

    @Test
    void meteringDataAdministratorId() {
        var energinetRegionalInformation = new EnerginetRegionalInformation();

        var meteringDataAdministratorId = energinetRegionalInformation.meteringDataAdministratorId();

        assertEquals("Energinet", meteringDataAdministratorId);
    }
}