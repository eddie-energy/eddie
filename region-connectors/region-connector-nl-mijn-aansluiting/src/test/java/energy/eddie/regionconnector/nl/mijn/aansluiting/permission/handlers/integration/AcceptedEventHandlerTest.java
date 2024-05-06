package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers.integration;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlInternalPollingEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedEventHandlerTest {

    @Mock
    private PollingService pollingService;
    @Mock
    private NlPermissionRequestRepository repository;

    @Test
    void testAccept_doesNotTriggerPolling_onInternalPermissionEvents() {
        // Given
        var eventBus = new EventBusImpl();
        new AcceptedEventHandler(eventBus, pollingService, repository);

        // When
        eventBus.emit(new NlInternalPollingEvent());

        // Then
        verify(pollingService, never()).fetchConsumptionData(any());

        // Clean-Up
        eventBus.close();
    }

    @Test
    void testAccept_doesTriggerPolling_onAcceptedEvent() {
        // Given
        var eventBus = new EventBusImpl();
        new AcceptedEventHandler(eventBus, pollingService, repository);
        var pr = new MijnAansluitingPermissionRequest("pid",
                                                      "cid",
                                                      "dnid",
                                                      PermissionProcessStatus.ACCEPTED,
                                                      "state",
                                                      "codeVerifier",
                                                      ZonedDateTime.now(ZoneOffset.UTC),
                                                      LocalDate.now(ZoneOffset.UTC).minusDays(10),
                                                      null,
                                                      Granularity.P1D);
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When
        eventBus.emit(new NlSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(pollingService).fetchConsumptionData(pr);

        // Clean-Up
        eventBus.close();
    }

    @Test
    void testAccept_doesNotTriggerPolling_onUnknownPermissionRequest() {
        // Given
        var eventBus = new EventBusImpl();
        new AcceptedEventHandler(eventBus, pollingService, repository);
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When
        eventBus.emit(new NlSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(pollingService, never()).fetchConsumptionData(any());

        // Clean-Up
        eventBus.close();
    }

    @Test
    void testAccept_doesNotTriggerPolling_onFuturePermissionRequest() {
        // Given
        var eventBus = new EventBusImpl();
        new AcceptedEventHandler(eventBus, pollingService, repository);
        var pr = new MijnAansluitingPermissionRequest("pid",
                                                      "cid",
                                                      "dnid",
                                                      PermissionProcessStatus.ACCEPTED,
                                                      "state",
                                                      "codeVerifier",
                                                      ZonedDateTime.now(ZoneOffset.UTC),
                                                      LocalDate.now(ZoneOffset.UTC).plusDays(10),
                                                      null,
                                                      Granularity.P1D);
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When
        eventBus.emit(new NlSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(pollingService, never()).fetchConsumptionData(any());

        // Clean-Up
        eventBus.close();
    }
}