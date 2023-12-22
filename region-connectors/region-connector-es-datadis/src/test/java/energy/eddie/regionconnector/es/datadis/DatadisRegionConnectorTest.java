package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DatadisRegionConnectorTest {
    @Test
    void terminatePermission_callsService() throws PermissionNotFoundException, StateTransitionException {
        // Given
        var mockService = mock(PermissionRequestService.class);
        var permissionId = "SomeId";

        var connector = new DatadisRegionConnector(mockService);

        // When
        assertDoesNotThrow(() -> connector.terminatePermission(permissionId));

        // Then
        verify(mockService).terminatePermission(permissionId);
    }


    @Test
    void getMetadata_returnExpectedMetadata() {
        // Given
        var mockService = mock(PermissionRequestService.class);

        var connector = new DatadisRegionConnector(mockService);

        // When
        var result = connector.getMetadata();

        // then
        assertEquals(DatadisRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void health_returnsHealthChecks() {
        // Given
        var mockService = mock(PermissionRequestService.class);

        var connector = new DatadisRegionConnector(mockService);
        var res = connector.health();

        assertEquals(Map.of("permissionRequestRepository", HealthState.UP), res);
    }
}
