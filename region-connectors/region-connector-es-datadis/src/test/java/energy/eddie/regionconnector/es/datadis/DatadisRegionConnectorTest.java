package energy.eddie.regionconnector.es.datadis;

import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatadisRegionConnectorTest {
    @Mock
    private PermissionRequestService mockService;
    @InjectMocks
    private DatadisRegionConnector connector;

    @Test
    void terminatePermission_callsService() throws PermissionNotFoundException {
        // Given
        var permissionId = "SomeId";

        // When
        assertDoesNotThrow(() -> connector.terminatePermission(permissionId));

        // Then
        verify(mockService).terminatePermission(permissionId);
    }

    @Test
    void terminateUnknownPermission_doesNothing() throws PermissionNotFoundException {
        // Given
        var permissionId = "SomeId";
        doThrow(new PermissionNotFoundException("SomeId"))
                .when(mockService).terminatePermission("SomeId");

        // When
        // Then
        assertDoesNotThrow(() -> connector.terminatePermission(permissionId));
    }

    @Test
    void getMetadata_returnExpectedMetadata() {
        // Given
        // When
        var result = connector.getMetadata();

        // then
        assertEquals(DatadisRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void health_returnsHealthChecks() {
        // Given
        // When
        var res = connector.health();

        // Then
        assertNotNull(res);
    }
}