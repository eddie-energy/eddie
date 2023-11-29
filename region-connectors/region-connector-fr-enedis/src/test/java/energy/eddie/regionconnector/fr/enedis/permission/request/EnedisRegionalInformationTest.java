package energy.eddie.regionconnector.fr.enedis.permission.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisRegionalInformationTest {

    @Test
    void countryCode() {
        var enedisRegionalInformation = new EnedisRegionalInformation();

        var countryCode = enedisRegionalInformation.countryCode();

        assertEquals("fr", countryCode);
    }

    @Test
    void permissionAdministratorId() {
        var enedisRegionalInformation = new EnedisRegionalInformation();

        var permissionAdministratorId = enedisRegionalInformation.permissionAdministratorId();

        assertEquals("Enedis", permissionAdministratorId);
    }

    @Test
    void meteringDataAdministratorId() {
        var enedisRegionalInformation = new EnedisRegionalInformation();

        var meteringDataAdministratorId = enedisRegionalInformation.meteringDataAdministratorId();

        assertEquals("Enedis", meteringDataAdministratorId);
    }

    @Test
    void regionConnectorId() {
        var enedisRegionalInformation = new EnedisRegionalInformation();

        var regionConnectorId = enedisRegionalInformation.regionConnectorId();

        assertEquals("fr-enedis", regionConnectorId);
    }
}