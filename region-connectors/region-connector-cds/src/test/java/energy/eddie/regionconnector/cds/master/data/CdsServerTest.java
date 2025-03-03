package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdsServerTest {
    @Test
    void testId_returnsCorrectIdAsString() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L)
                                              .setBaseUri("http://localhost")
                                              .setName("CDS Server")
                                              .setCoverages(Set.of(EnergyType.ELECTRICITY))
                                              .setClientId("client-id")
                                              .setClientSecret("client-secret")
                                              .setTokenEndpoint("http://localhost")
                                              .setAuthorizationEndpoint(null)
                                              .setParEndpoint(null)
                                              .build();

        // When
        var res = cdsServer.idAsString();

        // Then
        assertEquals("1", res);
    }

    @Test
    void testDisplayName_returnsCorrectDisplayName() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L)
                                              .setBaseUri("http://localhost")
                                              .setName("CDS Server")
                                              .setCoverages(Set.of(EnergyType.ELECTRICITY))
                                              .setClientId("client-id")
                                              .setClientSecret("client-secret")
                                              .setTokenEndpoint("http://localhost")
                                              .setAuthorizationEndpoint(null)
                                              .setParEndpoint(null)
                                              .build();

        // When
        var res = cdsServer.displayName();

        // Then
        assertEquals("CDS Server - http://localhost", res);
    }
}