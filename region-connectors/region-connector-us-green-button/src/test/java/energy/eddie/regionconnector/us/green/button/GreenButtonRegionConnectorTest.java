package energy.eddie.regionconnector.us.green.button;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GreenButtonRegionConnectorTest {
    @InjectMocks
    private GreenButtonRegionConnector regionConnector;


    @Test
    void testGetMetadata_returnsMetadata() {
        // Given
        // When
        var res = regionConnector.getMetadata();

        // Then
        assertEquals(GreenButtonRegionConnectorMetadata.getInstance(), res);
    }
}