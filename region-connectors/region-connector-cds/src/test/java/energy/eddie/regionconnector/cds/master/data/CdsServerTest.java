package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdsServerTest {
    @Test
    void testId_returnsCorrectId() {
        // Given
        var cdsServer = new CdsServer(1L, "http://localhost", "CDS Server", Set.of(EnergyType.ELECTRICITY), "client-id", "client-secret");

        // When
        var res = cdsServer.id();

        // Then
        assertEquals("1", res);
    }

    @Test
    void testDisplayName_returnsCorrectDisplayName() {
        // Given
        var cdsServer = new CdsServer(1L, "http://localhost", "CDS Server", Set.of(EnergyType.ELECTRICITY), "client-id", "client-secret");

        // When
        var res = cdsServer.displayName();

        // Then
        assertEquals("CDS Server - http://localhost", res);
    }
}