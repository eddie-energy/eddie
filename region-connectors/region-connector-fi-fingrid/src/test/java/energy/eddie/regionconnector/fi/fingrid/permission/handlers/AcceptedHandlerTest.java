package energy.eddie.regionconnector.fi.fingrid.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private FiPermissionRequestRepository repository;
    @Mock
    private PollingService pollingService;
    @Mock
    private DataNeedsService dataNeedsService;
    @SuppressWarnings("unused")
    @InjectMocks
    private AcceptedHandler handler;

    @Test
    void acceptedEvent_triggersPollingValidatedHistoricalData() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .createFingridPermissionRequest();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var dn = new ValidatedHistoricalDataDataNeed(new RelativeDuration(null, null, null),
                                                     EnergyType.ELECTRICITY,
                                                     Granularity.PT1H,
                                                     Granularity.P1Y);
        when(dataNeedsService.getById("dnid")).thenReturn(dn);

        // When
        eventBus.emit(new AcceptedEvent("pid"));

        // Then
        verify(pollingService).pollTimeSeriesData(pr);
    }

    @Test
    void acceptedEvent_triggersPollingAccountingPointData() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .createFingridPermissionRequest();
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        when(dataNeedsService.getById("dnid")).thenReturn(new AccountingPointDataNeed());

        // When
        eventBus.emit(new AcceptedEvent("pid"));

        // Then
        verify(pollingService).pollAccountingPointData(pr);
    }

    @Test
    void acceptedEvent_withInvalidDataNeed_doesNothing() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .createFingridPermissionRequest();
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        when(dataNeedsService.getById("dnid")).thenReturn(new AiidaDataNeed(Set.of()));

        // When
        eventBus.emit(new AcceptedEvent("pid"));

        // Then
        verifyNoInteractions(pollingService);
    }
}