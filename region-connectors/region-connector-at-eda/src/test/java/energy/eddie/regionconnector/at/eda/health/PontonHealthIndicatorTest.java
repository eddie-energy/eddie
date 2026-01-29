package energy.eddie.regionconnector.at.eda.health;

import energy.eddie.regionconnector.at.eda.ponton.messenger.HealthCheck;
import energy.eddie.regionconnector.at.eda.ponton.messenger.MessengerHealth;
import energy.eddie.regionconnector.at.eda.ponton.messenger.MessengerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PontonHealthIndicatorTest {
    @SuppressWarnings("unused")
    @Mock
    private MessengerHealth messengerHealth;
    @InjectMocks
    private PontonHealthIndicator indicator;

    @Test
    void healthReturnsUpWithDetails_whenPontonUp() {
        // Given
        var details = Map.of("PONTON", new HealthCheck("PONTON", true, "empty"));
        when(messengerHealth.messengerStatus())
                .thenReturn(new MessengerStatus(details, true));
        // When
        var res = indicator.health();

        // Then
        assertAll(
                () -> assertEquals(Status.UP, res.getStatus()),
                () -> assertEquals(details, res.getDetails())
        );
    }

    @Test
    void healthReturnsDownWithDetails_whenPontonDown() {
        // Given
        var details = Map.of("PONTON", new HealthCheck("PONTON", false, "empty"));
        when(messengerHealth.messengerStatus())
                .thenReturn(new MessengerStatus(details, false));
        // When
        var res = indicator.health();

        // Then
        assertAll(
                () -> assertEquals(Status.DOWN, res.getStatus()),
                () -> assertEquals(details, res.getDetails())
        );
    }
}