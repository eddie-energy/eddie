package energy.eddie.regionconnector.dk.energinet.permission.handler;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkAcceptedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
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
import java.time.ZonedDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptanceHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @Mock
    private DkPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptanceHandler acceptanceHandler;

    @Test
    void testAccept_triggersPolling() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                "mid",
                                                "refresh",
                                                now,
                                                now,
                                                Granularity.PT1H,
                                                null,
                                                PermissionProcessStatus.VALIDATED,
                                                ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        // When
        eventBus.emit(new DkAcceptedEvent("pid", "access"));

        // Then
        verify(pollingService).fetchHistoricalMeterReadings(pr);
    }
}