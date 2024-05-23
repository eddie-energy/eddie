package energy.eddie.regionconnector.dk.energinet.permission.handler;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkAcceptedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.services.AccountingPointDetailsService;
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
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptanceHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @Mock
    private AccountingPointDetailsService accountingPointDetailsService;
    @Mock
    private DkPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptanceHandler acceptanceHandler;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private TimeframedDataNeed timeframedDataNeed;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;

    @Test
    void testAccept_triggersPolling() {
        // Given
        var pr = permissionRequest();
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(timeframedDataNeed));
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        // When
        eventBus.emit(new DkAcceptedEvent("pid", "access"));

        // Then
        verify(pollingService).fetchHistoricalMeterReadings(pr);
    }

    private EnerginetPermissionRequest permissionRequest() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        return new EnerginetPermissionRequest("pid",
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
    }

    @Test
    void testAccept_triggersAccountingPointService() {
        // Given
        var pr = permissionRequest();
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(accountingPointDataNeed));
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        // When
        eventBus.emit(new DkAcceptedEvent("pid", "access"));

        // Then
        verify(accountingPointDetailsService).fetchMeteringPointDetails(pr);
    }
}
