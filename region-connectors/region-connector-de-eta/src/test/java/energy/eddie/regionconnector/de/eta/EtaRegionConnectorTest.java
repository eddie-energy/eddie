package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EtaRegionConnector.
 */
@ExtendWith(MockitoExtension.class)
class EtaRegionConnectorTest {

    @Mock
    private DePermissionRequestRepository repository;

    @Mock
    private Outbox outbox;

    private EtaRegionConnector regionConnector;

    @BeforeEach
    void setUp() {
        regionConnector = new EtaRegionConnector(repository, outbox);
    }

    @Test
    void testGetMetadata() {
        var metadata = regionConnector.getMetadata();
        
        assertNotNull(metadata);
        assertEquals("de-eta", metadata.id());
        assertEquals("DE", metadata.countryCode());
        assertTrue(metadata.coveredMeteringPoints() > 0);
        assertFalse(metadata.supportedGranularities().isEmpty());
        assertFalse(metadata.supportedEnergyTypes().isEmpty());
    }

    @Test
    void testTerminatePermissionSuccess() {
        String permissionId = UUID.randomUUID().toString();
        DePermissionRequest mockRequest = mock(DePermissionRequest.class);
        
        when(repository.findByPermissionId(permissionId))
            .thenReturn(Optional.of(mockRequest));

        regionConnector.terminatePermission(permissionId);

        verify(repository).findByPermissionId(permissionId);
        verify(outbox, times(2)).commit(any());
    }

    @Test
    void testTerminatePermissionNotFound() {
        String permissionId = UUID.randomUUID().toString();
        
        when(repository.findByPermissionId(permissionId))
            .thenReturn(Optional.empty());

        regionConnector.terminatePermission(permissionId);

        verify(repository).findByPermissionId(permissionId);
        verify(outbox, never()).commit(any());
    }

    @Test
    void testRegionConnectorIdConsistency() {
        assertEquals("de-eta", EtaRegionConnectorMetadata.REGION_CONNECTOR_ID);
        assertEquals("de-eta", regionConnector.getMetadata().id());
    }
}
