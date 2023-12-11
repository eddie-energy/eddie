package energy.eddie.regionconnector.at.eda.permission.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EdaRegionalInformationTest {

    @Test
    void countryCode() {
        EdaRegionalInformation edaRegionalInformation = new EdaRegionalInformation("dsoId");
        assertEquals("at", edaRegionalInformation.countryCode());
    }

    @Test
    void regionConnectorId() {
        EdaRegionalInformation edaRegionalInformation = new EdaRegionalInformation("dsoId");
        assertEquals("at-eda", edaRegionalInformation.regionConnectorId());
    }

    @Test
    void permissionAdministratorId_returnsDsoId() {
        String expected = "dsoId";
        EdaRegionalInformation edaRegionalInformation = new EdaRegionalInformation(expected);

        assertEquals(expected, edaRegionalInformation.permissionAdministratorId());
    }

    @Test
    void meteringDataAdministratorId_returnsDsoId() {
        String expected = "dsoId";
        EdaRegionalInformation edaRegionalInformation = new EdaRegionalInformation(expected);

        assertEquals(expected, edaRegionalInformation.meteringDataAdministratorId());
    }
}