package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeoutServiceTest {
    @Mock
    private FrPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @SuppressWarnings("unused")
    @Spy
    private TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration(24);
    @InjectMocks
    private TimeoutService timeoutService;

    @Test
    void testTimeoutPendingPermissionRequests_transitionsRequests() {
        // Given
        var start = LocalDate.now(ZONE_ID_FR);
        var end = LocalDate.now(ZONE_ID_FR).plusDays(10);
        EnedisPermissionRequest request = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                start,
                end,
                Granularity.P1D,
                PermissionProcessStatus.VALIDATED,
                null,
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                UsagePointType.CONSUMPTION
        );

        when(repository.findTimedOutPermissionRequests(24))
                .thenReturn(List.of(request));
        // When
        timeoutService.timeoutPendingPermissionRequests();

        // Then
        verify(outbox, times(2)).commit(any());
    }

    @Test
    void testTimeoutPendingPermissionRequests_doesNotThrow() {
        // Given
        var start = LocalDate.now(ZONE_ID_FR);
        var end = LocalDate.now(ZONE_ID_FR).plusDays(10);
        EnedisPermissionRequest request = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                start,
                end,
                Granularity.P1D,
                PermissionProcessStatus.VALIDATED,
                null,
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                UsagePointType.CONSUMPTION
        );
        when(repository.findTimedOutPermissionRequests(24))
                .thenReturn(List.of(request));

        // When
        // Then
        assertDoesNotThrow(timeoutService::timeoutPendingPermissionRequests);
    }
}
