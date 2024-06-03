package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeoutServiceTest {
    @Spy
    @SuppressWarnings("unused")
    private final TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration(24);
    @Mock
    private Outbox outbox;
    @Mock
    private NlPermissionRequestRepository repository;
    @InjectMocks
    private TimeoutService timeoutService;
    @Captor
    private ArgumentCaptor<NlSimpleEvent> simpleCaptor;

    @Test
    void testTimeout_emitsStalePermissionRequests() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.findStalePermissionRequests(24))
                .thenReturn(List.of(
                        new MijnAansluitingPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                "",
                                "verifier",
                                ZonedDateTime.now(ZoneOffset.UTC),
                                now,
                                now,
                                Granularity.PT1H
                        )
                ));

        // When
        timeoutService.timeout();

        // Then
        verify(outbox, times(2)).commit(simpleCaptor.capture());
        var res = simpleCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(PermissionProcessStatus.TIMED_OUT, res.status())
        );
    }
}
