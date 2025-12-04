package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.de.eta.client.DeEtaPaApiClient;
import energy.eddie.regionconnector.de.eta.permission.events.UnableToSendEvent;
import energy.eddie.regionconnector.de.eta.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.de.eta.permission.requests.DateRange;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidatedEventHandlerTest {

    @Mock
    private DeEtaPaApiClient apiClient;
    @Mock
    private DeEtaPermissionRequestRepository repository;
    @Mock
    private DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @Mock
    private Outbox outbox;

    @SuppressWarnings("unused")
    @InjectMocks
    private ValidatedEventHandler validatedEventHandler;

    private DeEtaPermissionRequest buildPermissionRequest(String pid, String cid, String dnid) {
        var created = ZonedDateTime.now(ZoneOffset.UTC);
        var dr = new DateRange(LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC));
        return new DeEtaPermissionRequest(pid, cid, dnid, PermissionProcessStatus.VALIDATED, created, dr, Granularity.P1D.name());
    }

    @BeforeEach
    void setupDataNeed() {
        // Ensure handler calculates timeframe path without errors
        var tf = new Timeframe(LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC));
        when(dataNeedCalculationService.calculate(any(String.class), any())).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.P1D), tf, tf)
        );
    }

    @Test
    void testAccept_paRespondsFailure_commitsUnableToSendEvent() {
        // Given
        var pid = "pid-1";
        when(repository.getByPermissionId(pid)).thenReturn(buildPermissionRequest(pid, "cid-1", "dn-1"));
        when(apiClient.sendPermissionRequest(any())).thenReturn(Mono.just(new DeEtaPaApiClient.SendPermissionResponse(false)));

        ArgumentCaptor<UnableToSendEvent> captor = ArgumentCaptor.forClass(UnableToSendEvent.class);

        // When
        eventBus.emit(new ValidatedEvent(pid, null, null, null, null, null));

        // Then
        verify(outbox, timeout(5000)).commit(captor.capture());
        var event = captor.getValue();
        assertTrue(event.reason() != null && !event.reason().isBlank(), "Reason should be populated");
    }

    @Test
    void testAccept_exception_commitsUnableToSendEventWithReason() {
        // Given
        var pid = "pid-2";
        when(repository.getByPermissionId(pid)).thenReturn(buildPermissionRequest(pid, "cid-2", "dn-2"));
        var ex = WebClientResponseException.create(500, "Error", null, "boom".getBytes(), null);
        when(apiClient.sendPermissionRequest(any())).thenReturn(Mono.error(ex));

        ArgumentCaptor<UnableToSendEvent> captor = ArgumentCaptor.forClass(UnableToSendEvent.class);

        // When
        eventBus.emit(new ValidatedEvent(pid, null, null, null, null, null));

        // Then
        verify(outbox, timeout(5000)).commit(captor.capture());
        var event = captor.getValue();
        assertTrue(event.reason().contains("HTTP 500") || event.reason().contains("boom"));
    }
}
