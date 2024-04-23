package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @Mock
    private HistoricalDataService historicalDataService;
    @Mock
    private FrPermissionRequestRepository permissionRequestRepository;
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler acceptedHandler;

    @Test
    void testAccept_fetchesHistoricalData() {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        var end = LocalDate.now(ZoneOffset.UTC).minusDays(5);
        var pr = new EnedisPermissionRequest(
                "pid", "cid", "dnid", start, end, Granularity.PT30M, PermissionProcessStatus.ACCEPTED
        );
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When
        eventBus.emit(new FrAcceptedEvent("pid", "usagePointId"));

        // Then
        verify(historicalDataService).fetchHistoricalMeterReadings(pr, "usagePointId");
    }

    @Test
    void testAccept_doesNotFetchHistoricalData_onUnknownPermissionRequest() {
        // Given
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When
        eventBus.emit(new FrAcceptedEvent("pid", "usagePointId"));

        // Then
        verify(historicalDataService, never()).fetchHistoricalMeterReadings(any(), any());
    }
}