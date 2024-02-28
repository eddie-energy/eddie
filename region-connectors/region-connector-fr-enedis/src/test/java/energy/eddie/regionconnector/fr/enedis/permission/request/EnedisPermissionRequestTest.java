package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisMalformedState;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnedisPermissionRequestTest {
    @Test
    void constructorWithPermissionId_setsPermissionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);

        // Then
        assertEquals(permissionId, request.permissionId());
    }

    @Test
    void constructorWithConnectionId_setsConnectionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);

        // Then
        assertEquals(connectionId, request.connectionId());
    }

    @Test
    void constructorWithoutPermissionId_generatesPermissionId() {
        // Given
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(connectionId, "dnid", start, end, Granularity.P1D);

        // Then
        assertNotNull(request.permissionId());
    }

    @Test
    void constructorWithContextStart_setsStart() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);

        // Then
        assertEquals(start, request.start());
    }

    @Test
    void constructorWithContextEnd_setsEnd() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);

        // Then
        assertEquals(end, request.end());
    }

    @Test
    void constructorWithContextConnectionId_setConnectionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);

        // Then
        assertEquals("testConnectionId", request.connectionId());
    }

    @Test
    void changeState_updatesState() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);
        PermissionRequestState newState = new FrEnedisMalformedState(request, List.of());

        // When
        request.changeState(newState);

        // Then
        assertEquals(newState, request.state());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void setUsagePointId_setsUsagePointId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);
        String usagePointId = "testUsagePointId";

        // When
        request.setUsagePointId(usagePointId);

        // Then
        assertEquals(usagePointId, request.usagePointId().get());
    }

    @Test
    void usagePointIdIsNotSet_returnsEmpty() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D);

        // Then
        assertFalse(request.usagePointId().isPresent());
    }
}