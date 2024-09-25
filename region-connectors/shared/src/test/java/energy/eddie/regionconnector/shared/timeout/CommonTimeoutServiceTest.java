package energy.eddie.regionconnector.shared.timeout;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.PermissionEventFactory;
import energy.eddie.regionconnector.shared.event.sourcing.TestEvent;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonTimeoutServiceTest {
    @SuppressWarnings("unused")
    @Spy
    private final TimeoutConfiguration configuration = new TimeoutConfiguration(168);
    @Mock
    private StalePermissionRequestRepository<PermissionRequest> repository;
    @Mock
    private PermissionEventFactory factory;
    @Mock
    private Outbox outbox;
    @SuppressWarnings("unused")
    @Mock
    private RegionConnectorMetadata metadata;
    @InjectMocks
    private CommonTimeoutService timeoutService;
    @Captor
    private ArgumentCaptor<PermissionEvent> eventCaptor;

    @Test
    void testTimeout_emitsSentToPAEventAndTimeoutEvents_whenPermissionRequestHasStatusValidated() {
        // Given
        PermissionRequest pr = new SimplePermissionRequest("pid",
                                                           "cid",
                                                           "dnid",
                                                           PermissionProcessStatus.VALIDATED);
        when(repository.findStalePermissionRequests(168))
                .thenReturn(List.of(pr));
        when(factory.create(eq("pid"), any()))
                .thenReturn(new TestEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR))
                .thenReturn(new TestEvent("pid", PermissionProcessStatus.TIMED_OUT));

        // When
        timeoutService.timeout();

        // Then
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var res1 = eventCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", res1.permissionId()),
                () -> assertEquals(PermissionProcessStatus.TIMED_OUT, res1.status())
        );

        var res2 = eventCaptor.getAllValues().getFirst();
        assertAll(
                () -> assertEquals("pid", res2.permissionId()),
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, res2.status())
        );
    }

    @Test
    void testTimeout_emitsTimeoutEvent_whenPermissionRequestHasStatusSentToPermissionAdministrator() {
        // Given
        PermissionRequest pr = new SimplePermissionRequest("pid",
                                                           "cid",
                                                           "dnid",
                                                           PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        when(repository.findStalePermissionRequests(168))
                .thenReturn(List.of(pr));
        when(factory.create(eq("pid"), any()))
                .thenReturn(new TestEvent("pid", PermissionProcessStatus.TIMED_OUT));

        // When
        timeoutService.timeout();

        // Then
        verify(outbox, times(1)).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(PermissionProcessStatus.TIMED_OUT, res.status())
        );
    }
}