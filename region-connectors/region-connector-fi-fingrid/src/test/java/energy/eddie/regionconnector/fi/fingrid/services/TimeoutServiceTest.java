package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeoutServiceTest {

    @Mock
    private FiPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @Spy
    @SuppressWarnings("unused")
    private TimeoutConfiguration config = new TimeoutConfiguration(1);
    @InjectMocks
    private TimeoutService timeoutService;
    @Captor
    private ArgumentCaptor<SimpleEvent> eventCaptor;

    @Test
    void testTimeout() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        when(repository.findStalePermissionRequests(1))
                .thenReturn(List.of(
                        new FingridPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                PermissionProcessStatus.VALIDATED,
                                now.minusHours(10),
                                now.toLocalDate(),
                                now.toLocalDate().plusDays(1),
                                "identifier"
                        )
                ));

        // When
        timeoutService.timeout();

        // Then
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var event = eventCaptor.getAllValues().getFirst();
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, event.status());
        event = eventCaptor.getValue();
        assertEquals(PermissionProcessStatus.TIMED_OUT, event.status());
    }
}