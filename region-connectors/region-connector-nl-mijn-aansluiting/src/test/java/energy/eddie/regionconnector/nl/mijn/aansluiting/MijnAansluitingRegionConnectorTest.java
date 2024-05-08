package energy.eddie.regionconnector.nl.mijn.aansluiting;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.ApiClient;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.TerminationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MijnAansluitingRegionConnectorTest {
    @Mock
    private TerminationService terminationService;
    @Mock
    private ApiClient apiClient;
    @InjectMocks
    private MijnAansluitingRegionConnector regionConnector;

    @Test
    void testTermination_terminatesPermissionRequest() {
        // Given
        // When
        regionConnector.terminatePermission("pid");

        // Then
        verify(terminationService).terminate("pid");
    }

    @Test
    void testGetMetadata_returnsMetadata() {
        // Given
        // When
        var res = regionConnector.getMetadata();

        // Then
        assertEquals(MijnAansluitingRegionConnectorMetadata.getInstance(), res);
    }

    @Test
    void testHealth_returnsHealth() {
        // Given
        when(apiClient.health())
                .thenReturn(Map.of("MIJN_AANSLUITING", HealthState.UP));

        // When
        var res = regionConnector.health();

        // Then
        assertEquals(Map.of("MIJN_AANSLUITING", HealthState.UP), res);
    }
}