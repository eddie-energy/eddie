package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisPermissionRequestTest {
    @Test
    void constructorWithPermissionId_setsPermissionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId,
                                                                      connectionId,
                                                                      "dnid",
                                                                      start,
                                                                      end,
                                                                      Granularity.P1D,
                                                                      PermissionProcessStatus.CREATED);

        // Then
        assertEquals(permissionId, request.permissionId());
    }

    @Test
    void constructorWithConnectionId_setsConnectionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId,
                                                                      connectionId,
                                                                      "dnid",
                                                                      start,
                                                                      end,
                                                                      Granularity.P1D,
                                                                      PermissionProcessStatus.CREATED);

        // Then
        assertEquals(connectionId, request.connectionId());
    }

    @Test
    void constructorWithContextStart_setsStart() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end,
                                                                      Granularity.P1D, PermissionProcessStatus.CREATED);

        // Then
        assertEquals(start, request.start());
    }

    @Test
    void constructorWithContextEnd_setsEnd() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end,
                                                                      Granularity.P1D, PermissionProcessStatus.CREATED);

        // Then
        assertEquals(end, request.end());
    }

    @Test
    void constructorWithContextConnectionId_setConnectionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end,
                                                                      Granularity.P1D, PermissionProcessStatus.CREATED);

        // Then
        assertEquals("testConnectionId", request.connectionId());
    }
}
