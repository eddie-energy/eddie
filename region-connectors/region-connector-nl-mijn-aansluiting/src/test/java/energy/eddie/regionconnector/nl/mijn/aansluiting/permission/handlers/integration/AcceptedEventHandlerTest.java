package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers.integration;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlInternalPollingEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedEventHandlerTest {
    public static final ValidatedHistoricalDataDataNeed DATA_NEED = new ValidatedHistoricalDataDataNeed(
            new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
            EnergyType.ELECTRICITY,
            Granularity.P1D,
            Granularity.P1D
    );
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @Mock
    private NlPermissionRequestRepository repository;
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedEventHandler acceptedEventHandler;

    @Test
    void testAccept_doesNotTriggerPolling_onInternalPermissionEvents() {
        // Given

        // When
        eventBus.emit(new NlInternalPollingEvent());

        // Then
        verify(pollingService, never()).pollTimeSeriesData(any());
    }

    @Test
    void testAcceptWithValidatedHistoricalDataNeed_doesTriggerPolling_onAcceptedEvent() {
        // Given
        when(dataNeedsService.getById("dnid"))
                .thenReturn(DATA_NEED);
        var pr = new MijnAansluitingPermissionRequest("pid",
                                                      "cid",
                                                      "dnid",
                                                      PermissionProcessStatus.ACCEPTED,
                                                      "state",
                                                      "codeVerifier",
                                                      ZonedDateTime.now(ZoneOffset.UTC),
                                                      LocalDate.now(ZoneOffset.UTC).minusDays(10),
                                                      null,
                                                      Granularity.P1D, "11", "999AB");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        eventBus.emit(new NlSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(pollingService).pollTimeSeriesData(pr);
    }

    @Test
    void testAcceptWithAccountingPointDataNeed_doesTriggerPolling_onAcceptedEvent() {
        // Given
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new AccountingPointDataNeed());
        var pr = new MijnAansluitingPermissionRequest("pid",
                                                      "cid",
                                                      "dnid",
                                                      PermissionProcessStatus.ACCEPTED,
                                                      "state",
                                                      "codeVerifier",
                                                      ZonedDateTime.now(ZoneOffset.UTC),
                                                      LocalDate.now(ZoneOffset.UTC).minusDays(10),
                                                      null,
                                                      Granularity.P1D, "11", "999AB");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        eventBus.emit(new NlSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(pollingService).fetchAccountingPointData(pr);
    }

    @Test
    void testAccept_doesNotTriggerPolling_onFuturePermissionRequest() {
        // Given
        when(dataNeedsService.getById("dnid"))
                .thenReturn(DATA_NEED);
        var pr = new MijnAansluitingPermissionRequest("pid",
                                                      "cid",
                                                      "dnid",
                                                      PermissionProcessStatus.ACCEPTED,
                                                      "state",
                                                      "codeVerifier",
                                                      ZonedDateTime.now(ZoneOffset.UTC),
                                                      LocalDate.now(ZoneOffset.UTC).plusDays(10),
                                                      null,
                                                      Granularity.P1D, "11", "999AB");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        eventBus.emit(new NlSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(pollingService, never()).pollTimeSeriesData(any());
    }
}