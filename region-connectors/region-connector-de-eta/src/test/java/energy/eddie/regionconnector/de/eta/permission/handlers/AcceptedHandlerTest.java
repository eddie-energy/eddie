package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.StartPollingEvent;
import energy.eddie.regionconnector.de.eta.service.PollingService;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {

    @Spy
    private final EventBus eventBus = new EventBusImpl();

    @Mock
    private DePermissionRequestRepository repository;

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
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .start(LocalDate.now(ZoneOffset.UTC).minusDays(10))
                .end(LocalDate.now(ZoneOffset.UTC))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .build();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));
        var dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );
        when(dataNeedsService.getById("dnid")).thenReturn(dataNeed);

        // When
        eventBus.emit(new AcceptedEvent("pid"));

        // Then
        verify(pollingService).pollTimeSeriesData(pr);
    }

    @Test
    void startPollingEvent_triggersPollingValidatedHistoricalData() {
        // Given
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .start(LocalDate.now(ZoneOffset.UTC).minusDays(10))
                .end(LocalDate.now(ZoneOffset.UTC))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .build();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));
        var dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );
        when(dataNeedsService.getById("dnid")).thenReturn(dataNeed);

        // When
        eventBus.emit(new StartPollingEvent("pid"));

        // Then
        verify(pollingService).pollTimeSeriesData(pr);
    }

    @Test
    void acceptedEvent_withUnsupportedDataNeed_doesNotTriggerPolling() {
        // Given
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .start(LocalDate.now(ZoneOffset.UTC).minusDays(10))
                .end(LocalDate.now(ZoneOffset.UTC))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .build();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));
        when(dataNeedsService.getById("dnid")).thenReturn(new AccountingPointDataNeed());

        // When
        eventBus.emit(new AcceptedEvent("pid"));

        // Then
        verifyNoInteractions(pollingService);
    }

    @Test
    void acceptedEvent_withMissingPermissionRequest_doesNotTriggerPolling() {
        // Given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When
        eventBus.emit(new AcceptedEvent("pid"));

        // Then
        verifyNoInteractions(pollingService);
        verifyNoInteractions(dataNeedsService);
    }
}

