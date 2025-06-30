package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlInternalPollingEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalPollingHandlerTest {
    @Mock
    private NlPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;

    @Captor
    private ArgumentCaptor<NlSimpleEvent> captor;

    @Test
    void testAccept_commitsFulfilled_onFulfilledPermissionRequest() {
        // Given
        var eventBus = new EventBusImpl();
        new InternalPollingHandler(eventBus, repository, outbox);
        var today = LocalDate.now(ZoneOffset.UTC);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new MijnAansluitingPermissionRequest("pid",
                                                      "cid",
                                                      "dnid",
                                                      PermissionProcessStatus.ACCEPTED,
                                                      "",
                                                      "",
                                                      now.minusDays(10),
                                                      today.minusDays(10),
                                                      today.minusDays(1),
                                                      Granularity.P1D, "11", "999AB");
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));

        // When
        eventBus.emit(new NlInternalPollingEvent("pid", Map.of("mid", now)));

        // Then
        verify(outbox).commit(captor.capture());
        assertEquals(PermissionProcessStatus.FULFILLED, captor.getValue().status());
        // Clean-Up
        eventBus.close();
    }

    @Test
    void testAccept_doesNotCommitFulfilled_onUnfulfilledPermissionRequest() {
        // Given
        var eventBus = new EventBusImpl();
        new InternalPollingHandler(eventBus, repository, outbox);
        var today = LocalDate.now(ZoneOffset.UTC);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new MijnAansluitingPermissionRequest("pid",
                                                      "cid",
                                                      "dnid",
                                                      PermissionProcessStatus.ACCEPTED,
                                                      "",
                                                      "",
                                                      now.minusDays(10),
                                                      today.minusDays(10),
                                                      today.plusDays(1),
                                                      Granularity.P1D, "11", "999AB");
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));

        // When
        eventBus.emit(new NlInternalPollingEvent("pid", Map.of("mid", now)));

        // Then
        verify(outbox, never()).commit(any());

        // Clean-Up
        eventBus.close();
    }

    @Test
    void testAccept_doesNotCommitFulfilled_onUnknownPermissionRequest() {
        // Given
        var eventBus = new EventBusImpl();
        new InternalPollingHandler(eventBus, repository, outbox);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When
        eventBus.emit(new NlInternalPollingEvent("pid", Map.of("mid", now)));

        // Then
        verify(outbox, never()).commit(any());

        // Clean-Up
        eventBus.close();
    }
}