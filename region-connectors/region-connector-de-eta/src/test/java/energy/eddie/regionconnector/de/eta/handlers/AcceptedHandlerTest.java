package energy.eddie.regionconnector.de.eta.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.handlers.AcceptedHandler;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {

    @Mock
    private DePermissionRequestRepository repository;
    @Mock
    private EtaPlusApiClient apiClient;
    @Mock
    private ValidatedHistoricalDataStream stream;
    @Mock
    private Outbox outbox;
    @Mock
    private EventBus eventBus;

    private AcceptedHandler handler;

    @BeforeEach
    void setUp() {
        // Prevent NullPointerException during constructor initialization
        when(eventBus.filteredFlux(AcceptedEvent.class)).thenReturn(Flux.empty());

        handler = new AcceptedHandler(
                eventBus,
                repository,
                apiClient,
                stream,
                outbox
        );
    }

    @Test
    void should_fetch_and_publish_data_when_request_is_valid_and_in_past() {
        // --- ARRANGE ---
        String permissionId = "perm-123";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        // 1. Mock Request
        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);
        // Ensure start date is yesterday to pass the "future data" check
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        // 2. Create concrete Data object (EtaPlusMeteredData is a record, so we instantiate it)
        EtaPlusMeteredData realData = new EtaPlusMeteredData(
                "meter-abc",
                LocalDate.now(),
                LocalDate.now(),
                Collections.emptyList(),
                "{}"
        );

        // 3. Mock API to return the concrete type
        when(apiClient.fetchMeteredData(request)).thenReturn(Mono.just(realData));

        // --- ACT ---
        handler.accept(event);

        // --- ASSERT ---
        verify(apiClient).fetchMeteredData(request);
        // Verify the stream received the specific concrete data object
        verify(stream).publish(request, realData);
        verifyNoInteractions(outbox);
    }

    @Test
    void should_ignore_request_if_permission_not_found() {
        // --- ARRANGE ---
        String permissionId = "perm-missing";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // --- ACT ---
        handler.accept(event);

        // --- ASSERT ---
        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
    }

    @Test
    void should_skip_fetching_if_start_date_is_in_future() {
        // --- ARRANGE ---
        String permissionId = "perm-future";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);
        // Set start date to tomorrow
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).plusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        // --- ACT ---
        handler.accept(event);

        // --- ASSERT ---
        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
    }

    @Test
    void should_emit_revoked_event_on_forbidden_error() {
        // --- ARRANGE ---
        String permissionId = "perm-revoked";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        // Simulate a 403 Forbidden error from the API
        // Using Spring's HttpClientErrorException.create for accurate simulation
        HttpClientErrorException forbiddenError = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null
        );
        when(apiClient.fetchMeteredData(request)).thenReturn(Mono.error(forbiddenError));

        // --- ACT ---
        handler.accept(event);

        // --- ASSERT ---
        ArgumentCaptor<SimpleEvent> eventCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox).commit(eventCaptor.capture());

        SimpleEvent capturedEvent = eventCaptor.getValue();
        assertEquals(permissionId, capturedEvent.permissionId());
        assertEquals(PermissionProcessStatus.REVOKED, capturedEvent.status());
    }

    @Test
    void should_log_error_but_not_revoke_on_generic_error() {
        // --- ARRANGE ---
        String permissionId = "perm-error";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        // Simulate a generic RuntimeException
        when(apiClient.fetchMeteredData(request)).thenReturn(Mono.error(new RuntimeException("Network Error")));

        // --- ACT ---
        handler.accept(event);

        // --- ASSERT ---
        verify(apiClient).fetchMeteredData(request);
        verifyNoInteractions(stream);
        verifyNoInteractions(outbox);
    }
}