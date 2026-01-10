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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

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
    void should_fetch_and_publish_data_in_chunks_when_request_is_valid() {
        String permissionId = "perm-123";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);

        when(request.meteringPointId()).thenReturn("DE_METER_123");
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).minusDays(30));
        when(request.end()).thenReturn(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        List<EtaPlusMeteredData.MeterReading> mockReadings = IntStream.range(0, 2500)
                .mapToObj(i -> new EtaPlusMeteredData.MeterReading(
                        "2023-01-01T00:00:00Z",
                        (double) i,
                        "kWh",
                        "VALIDATED"
                ))
                .toList();

        when(apiClient.streamMeteredData(request)).thenReturn(Flux.fromIterable(mockReadings));

        handler.accept(event);

        verify(apiClient).streamMeteredData(request);

        ArgumentCaptor<EtaPlusMeteredData> dataCaptor = ArgumentCaptor.forClass(EtaPlusMeteredData.class);

        verify(stream, times(3)).publish(eq(request), dataCaptor.capture());
        verifyNoInteractions(outbox);

        List<EtaPlusMeteredData> capturedChunks = dataCaptor.getAllValues();
        assertEquals(1000, capturedChunks.get(0).readings().size(), "First chunk should be 1000");
        assertEquals(1000, capturedChunks.get(1).readings().size(), "Second chunk should be 1000");
        assertEquals(500, capturedChunks.get(2).readings().size(), "Last chunk should be 500");
    }

    @Test
    void should_ignore_request_if_permission_not_found() {
        String permissionId = "perm-missing";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        handler.accept(event);

        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
    }

    @Test
    void should_skip_fetching_if_start_date_is_in_future() {
        String permissionId = "perm-future";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).plusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        handler.accept(event);

        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
    }

    @Test
    void should_emit_revoked_event_on_forbidden_error() {
        String permissionId = "perm-revoked";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        HttpClientErrorException forbiddenError = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null
        );

        when(apiClient.streamMeteredData(request)).thenReturn(Flux.error(forbiddenError));

        handler.accept(event);

        ArgumentCaptor<SimpleEvent> eventCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox).commit(eventCaptor.capture());

        SimpleEvent capturedEvent = eventCaptor.getValue();
        assertEquals(permissionId, capturedEvent.permissionId());
        assertEquals(PermissionProcessStatus.REVOKED, capturedEvent.status());
    }

    @Test
    void should_log_error_but_not_revoke_on_generic_error() {
        String permissionId = "perm-error";
        AcceptedEvent event = new AcceptedEvent(permissionId);

        DePermissionRequest request = mock(DePermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);
        when(request.start()).thenReturn(LocalDate.now(ZoneId.of("UTC")).minusDays(1));

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        when(apiClient.streamMeteredData(request)).thenReturn(Flux.error(new RuntimeException("Network Error")));

        handler.accept(event);

        verify(apiClient).streamMeteredData(request);
        verifyNoInteractions(stream);
        verifyNoInteractions(outbox);
    }
}