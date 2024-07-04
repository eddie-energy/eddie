package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.services.AccountingPointDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @Mock
    private AccountingPointDataService accountingPointDataService;
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler acceptedHandler;

    @Test
    void testAccept_fetchesMeteringPointSegment() {
        // Given
        String pid = "pid";
        String usagePointId = "usagePointId";

        // When
        eventBus.emit(new FrAcceptedEvent(pid, usagePointId));

        // Then
        verify(accountingPointDataService).fetchMeteringPointSegment(pid, usagePointId);
    }
}
