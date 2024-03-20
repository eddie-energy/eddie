package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisMalformedState;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.*;

class EnedisPermissionRequestTest {
    @Test
    void constructorWithPermissionId_setsPermissionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);

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
        StateBuilderFactory factory = new StateBuilderFactory();

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);

        // Then
        assertEquals(connectionId, request.connectionId());
    }

    @Test
    void constructorWithoutPermissionId_generatesPermissionId() {
        // Given
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(connectionId, "dnid", start, end, Granularity.P1D, factory);

        // Then
        assertNotNull(request.permissionId());
    }

    @Test
    void constructorWithContextStart_setsStart() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);

        // Then
        assertEquals(start, request.start().toLocalDate());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void constructorWithContextEnd_setsEnd() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);

        // Then
        assertEquals(end, request.end().toLocalDate());
    }

    @Test
    void constructorWithContextConnectionId_setConnectionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);

        // Then
        assertEquals("testConnectionId", request.connectionId());
    }

    @Test
    void changeState_updatesState() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);
        PermissionRequestState newState = new FrEnedisMalformedState(request, null);

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
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);
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
        LocalDate start = LocalDate.now(ZONE_ID_FR);
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, "dnid", start, end, Granularity.P1D, factory);

        // Then
        assertFalse(request.usagePointId().isPresent());
    }
}
