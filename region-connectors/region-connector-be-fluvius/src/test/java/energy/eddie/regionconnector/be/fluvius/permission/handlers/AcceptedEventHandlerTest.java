package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.regionconnector.be.fluvius.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.service.polling.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AcceptedEventHandlerTest {
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @SuppressWarnings("unused")
    @InjectMocks
    private AcceptedEventHandler handler;
    @Mock
    private PollingService pollingService;

    @Test
    void testAccept_acceptedEventTriggersPoll() {
        // When
        var meterReading = new MeterReading("pid", "ean", null);
        eventBus.emit(new AcceptedEvent("pid", List.of(meterReading)));

        // Then
        verify(pollingService).poll("pid");
    }

}