package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeoutServiceTest {
    @SuppressWarnings("unused")
    @Spy
    private final TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration(24);
    @Mock
    private Outbox outbox;
    @Mock
    private AiidaPermissionRequestViewRepository permissionRequestViewRepository;
    @InjectMocks
    private TimeoutService timeoutService;

    @Test
    void testTimeout_timeoutsStalePermissionRequests() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(permissionRequestViewRepository.findStalePermissionRequests(24))
                .thenReturn(List.of(
                        new AiidaPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                now,
                                now.plusDays(10),
                                PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                "termination-topic",
                                "username",
                                null,
                                Instant.now(Clock.systemUTC()).minus(25, ChronoUnit.HOURS)
                        )
                ));

        // When
        timeoutService.timeout();

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.TIMED_OUT, event.status())));
    }
}