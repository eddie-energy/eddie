package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.regionconnector.cds.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    PollingService pollingService;
    @Mock
    private CdsPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler handler;

    @Test
    void testAccept_pollsDataForPermissionRequest() {
        // Given
        var event = new AcceptedEvent("pid");
        var pr = new CdsPermissionRequestBuilder().build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        eventBus.emit(event);

        // Then
        verify(pollingService).poll(pr);
    }
}