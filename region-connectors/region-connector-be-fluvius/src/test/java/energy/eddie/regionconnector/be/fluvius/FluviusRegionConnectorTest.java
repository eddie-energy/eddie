package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.regionconnector.be.fluvius.service.TerminationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FluviusRegionConnectorTest {
    @Mock
    private TerminationService terminationService;
    @InjectMocks
    private FluviusRegionConnector fluviusRegionConnector;

    @Test
    void testTerminatePermission_callsTerminationService() {
        // Given
        // When
        fluviusRegionConnector.terminatePermission("pid");

        // Then
        verify(terminationService).terminate("pid");
    }
}