package energy.eddie.core.services;

import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedFeatureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SupportedFeatureServiceTest {
    @Mock
    private RegionConnectorSupportedFeatureExtension extension;

    @Test
    void testRegisterExtension_addsExtension() {
        // Given
        var service = new SupportedFeatureService();
        service.register(extension);

        // When
        var res = service.getSupportedFeatureExtensions();

        // Then
        assertEquals(List.of(extension), res);
    }
}