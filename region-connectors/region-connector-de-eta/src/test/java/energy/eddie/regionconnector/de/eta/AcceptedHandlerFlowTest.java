package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.handlers.AcceptedHandler;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerFlowTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private DePermissionRequestRepository repository;

    @Mock
    private EtaPlusApiClient apiClient;

    @Mock
    private Outbox outbox;

    private ValidatedHistoricalDataStream realStream;

    private AcceptedHandler acceptedHandler;

    @BeforeEach
    void setUp() {
        realStream = new ValidatedHistoricalDataStream(outbox);

        when(eventBus.filteredFlux(AcceptedEvent.class)).thenReturn(Flux.empty());

        acceptedHandler = new AcceptedHandler(
                eventBus,
                repository,
                apiClient,
                realStream,
                outbox,
                ObservationRegistry.NOOP
        );
    }

    private DePermissionRequest buildDefaultRequest(String permissionId, LocalDate start, LocalDate end) {
        return new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .connectionId("conn-1")
                .meteringPointId("malo-1")
                .start(start)
                .end(end)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID))
                .dataNeedId("need-1")
                .build();
    }

    // --- Happy path tests ---

    @Test
    @DisplayName("Should fetch data and emit LatestMeterReadingEvent when AcceptedEvent is received")
    void shouldFetchDataAndEmitLatestReadingEvent() {
        String permissionId = "perm-123";
        LocalDate start = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusMonths(3);
        LocalDate end = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusDays(1);

        DePermissionRequest mockRequest = buildDefaultRequest(permissionId, start, end);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(mockRequest));

        EtaPlusMeteredData.MeterReading reading = new EtaPlusMeteredData.MeterReading(
                end.atStartOfDay().toString(),
                123.45,
                "kWh",
                "VALIDATED"
        );

        EtaPlusMeteredData mockApiData = new EtaPlusMeteredData(
                "malo-1",
                start,
                end,
                List.of(reading)
        );

        when(apiClient.fetchMeteredData(mockRequest, "test-access-token")).thenReturn(Mono.just(mockApiData));

        AcceptedEvent event = new AcceptedEvent(permissionId, "test-access-token", null);
        acceptedHandler.accept(event);

        verify(apiClient).fetchMeteredData(mockRequest, "test-access-token");

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);

        verify(outbox, times(1)).commit(eventCaptor.capture());

        Object capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isInstanceOf(LatestMeterReadingEvent.class);

        LatestMeterReadingEvent readingEvent = (LatestMeterReadingEvent) capturedEvent;
        assertThat(readingEvent.permissionId()).isEqualTo(permissionId);

        assertThat(readingEvent.latestMeterReading()).isEqualTo(end);
    }

    @Test
    @DisplayName("Should skip historical data fetch when start date is in the future")
    void shouldSkipHistoricalDataFetchForFutureOnlyRequest() {
        String permissionId = "future-perm";
        LocalDate futureStart = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).plusDays(1);
        LocalDate futureEnd = futureStart.plusMonths(3);

        DePermissionRequest futureRequest = buildDefaultRequest(permissionId, futureStart, futureEnd);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(futureRequest));

        acceptedHandler.accept(new AcceptedEvent(permissionId, "test-access-token", null));

        verifyNoInteractions(apiClient);
        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("Should emit RevokedEvent when API returns 403 Forbidden")
    void shouldEmitRevokedEventWhenApiReturnsForbidden() {
        String permissionId = "perm-forbidden";
        LocalDate start = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusMonths(1);
        LocalDate end = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusDays(1);

        DePermissionRequest permissionRequest = buildDefaultRequest(permissionId, start, end);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        WebClientResponseException forbiddenException = WebClientResponseException.create(
                HttpStatus.FORBIDDEN.value(), "Forbidden", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );
        when(apiClient.fetchMeteredData(permissionRequest, "test-access-token")).thenReturn(Mono.error(forbiddenException));

        acceptedHandler.accept(new AcceptedEvent(permissionId, "test-access-token", null));

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SimpleEvent.class);
        assertThat(eventCaptor.getValue().status()).isEqualTo(PermissionProcessStatus.REVOKED);
        assertThat(eventCaptor.getValue().permissionId()).isEqualTo(permissionId);
    }

    @Test
    @DisplayName("Should commit UNABLE_TO_SEND when a generic API error occurs")
    void shouldCommitUnableToSendWhenGenericApiErrorOccurs() {
        String permissionId = "perm-error";
        LocalDate start = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusMonths(1);
        LocalDate end = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusDays(1);

        DePermissionRequest permissionRequest = buildDefaultRequest(permissionId, start, end);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(apiClient.fetchMeteredData(permissionRequest, "test-access-token")).thenReturn(Mono.error(new RuntimeException("unexpected API failure")));

        acceptedHandler.accept(new AcceptedEvent(permissionId, "test-access-token", null));

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SimpleEvent.class);
        assertThat(eventCaptor.getValue().status()).isEqualTo(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    // --- Resilience tests (subscription must survive all errors) ---

    @Test
    @DisplayName("Should skip event without crashing when permission request is not found")
    void shouldSkipEventWhenPermissionNotFound() {
        String unknownPermissionId = "unknown-id";
        when(repository.findByPermissionId(unknownPermissionId)).thenReturn(Optional.empty());

        acceptedHandler.accept(new AcceptedEvent(unknownPermissionId, "test-access-token", null));

        verifyNoInteractions(apiClient);
        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("Should commit UNABLE_TO_SEND when rate limited instead of throwing")
    void shouldCommitUnableToSendWhenRateLimited() {
        String permissionId = "perm-rate-limited";
        LocalDate start = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusMonths(1);
        LocalDate end = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusDays(1);

        DePermissionRequest permissionRequest = buildDefaultRequest(permissionId, start, end);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(apiClient.fetchMeteredData(permissionRequest, "test-access-token")).thenReturn(
                Mono.error(new RateLimitException("Rate limit exceeded for permission request " + permissionId))
        );

        acceptedHandler.accept(new AcceptedEvent(permissionId, "test-access-token", null));

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SimpleEvent.class);
        assertThat(eventCaptor.getValue().status()).isEqualTo(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    @DisplayName("Should not crash when outbox commit fails on 403")
    void shouldNotCrashWhenOutboxCommitFailsOnForbidden() {
        String permissionId = "perm-outbox-fail";
        LocalDate start = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusMonths(1);
        LocalDate end = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID).minusDays(1);

        DePermissionRequest permissionRequest = buildDefaultRequest(permissionId, start, end);

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        WebClientResponseException forbiddenException = WebClientResponseException.create(
                HttpStatus.FORBIDDEN.value(), "Forbidden", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );
        when(apiClient.fetchMeteredData(permissionRequest, "test-access-token")).thenReturn(Mono.error(forbiddenException));
        doThrow(new RuntimeException("DB connection lost")).when(outbox).commit(any());

        // Must not throw — subscription must survive
        acceptedHandler.accept(new AcceptedEvent(permissionId, "test-access-token", null));

        verify(outbox).commit(any());
    }
}