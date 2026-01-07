package energy.eddie.regionconnector.aiida.health;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttHealthIndicatorTest {
    @Mock
    private MqttAsyncClient mqttClient;
    @InjectMocks
    private MqttHealthIndicator healthIndicator;

    public static Stream<Arguments> healthUpOrDown_whenMqttUpOrDown() {
        return Stream.of(
                Arguments.of(true, Status.UP),
                Arguments.of(false, Status.DOWN)
        );
    }

    @ParameterizedTest
    @MethodSource
    void healthUpOrDown_whenMqttUpOrDown(boolean up, Status expected) {
        // Given
        when(mqttClient.isConnected()).thenReturn(up);

        // When
        var res = healthIndicator.health();

        // Then
        assertEquals(expected, res.getStatus());
    }
}