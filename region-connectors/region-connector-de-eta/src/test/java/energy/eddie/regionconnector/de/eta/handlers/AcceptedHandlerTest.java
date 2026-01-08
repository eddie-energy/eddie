package energy.eddie.regionconnector.de.eta.handlers;

// 1. Imports matching your working AcceptedHandler
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.client.EtaApiClient;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.persistence.DeMeterReadingTrackingEntity;
import energy.eddie.regionconnector.de.eta.persistence.DeMeterReadingTrackingRepository;
import energy.eddie.regionconnector.de.eta.providers.vhd.DeEtaMeteredData;
import energy.eddie.regionconnector.de.eta.providers.vhd.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.vhd.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;

// 2. Testing Imports
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // Useful for inspecting what was saved
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private DePermissionRequestRepository repository;
    @Mock private DataNeedsService dataNeedsService;
    @Mock private EtaApiClient apiClient;
    @Mock private ValidatedHistoricalDataStream stream;
    @Mock private DeMeterReadingTrackingRepository trackingRepository;

    private AcceptedHandler handler;

    @BeforeEach
    void setUp() {
        // Prevent the constructor from crashing or subscribing to a real Flux
        when(eventBus.filteredFlux(AcceptedEvent.class)).thenReturn(Flux.empty());

        handler = new AcceptedHandler(
                eventBus,
                repository,
                dataNeedsService,
                apiClient,
                stream,
                trackingRepository
        );
    }

    @Test
    void should_fetch_stream_and_persist_when_data_need_is_historical() {
        // --- ARRANGE ---
        String permissionId = "perm-123";
        UUID dataNeedId = UUID.randomUUID();

        // 1. Mock the Event
        AcceptedEvent event = mock(AcceptedEvent.class);
        when(event.permissionId()).thenReturn(permissionId);

        // 2. Mock Repository finding the request
        PermissionRequest request = mock(PermissionRequest.class);
        when(request.permissionId()).thenReturn(permissionId);
        when(request.dataNeedId()).thenReturn(dataNeedId);
        when(repository.getByPermissionId(permissionId)).thenReturn(request);

        // 3. Mock DataNeedService returning the CORRECT type
        // This triggers the switch case: case ValidatedHistoricalDataDataNeed ignored -> ...
        DataNeedInterface historicalNeed = new ValidatedHistoricalDataDataNeed();
        when(dataNeedsService.getById(dataNeedId)).thenReturn(historicalNeed);

        // 4. Mock API Client returning data
        Instant now = Instant.now();
        DeEtaMeteredData.Reading reading = new DeEtaMeteredData.Reading(
                now, new BigDecimal("500.00"), "kWh"
        );
        DeEtaMeteredData data = new DeEtaMeteredData("METER-ABC", List.of(reading));
        when(apiClient.getValidatedHistoricalData(request)).thenReturn(Mono.just(data));

        // 5. Mock DB Tracking (simulate first time seeing this permission)
        when(trackingRepository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // --- ACT ---
        handler.accept(event);

        // --- ASSERT ---
        // A. Verify data was streamed
        verify(stream).publish(any(IdentifiableValidatedHistoricalData.class));

        // B. Verify correct reading was saved to DB
        ArgumentCaptor<DeMeterReadingTrackingEntity> entityCaptor = ArgumentCaptor.forClass(DeMeterReadingTrackingEntity.class);
        verify(trackingRepository).save(entityCaptor.capture());

        DeMeterReadingTrackingEntity savedEntity = entityCaptor.getValue();
        assertEquals(permissionId, savedEntity.getPermissionId());
        assertEquals(now, savedEntity.getLatestReadingTime());
        assertEquals(new BigDecimal("500.00"), savedEntity.getLatestReadingValue());
    }

    @Test
    void should_ignore_request_if_data_need_is_not_historical() {
        // --- ARRANGE ---
        String permissionId = "perm-999";
        UUID dataNeedId = UUID.randomUUID();

        AcceptedEvent event = mock(AcceptedEvent.class);
        when(event.permissionId()).thenReturn(permissionId);

        PermissionRequest request = mock(PermissionRequest.class);
        when(request.dataNeedId()).thenReturn(dataNeedId);
        when(repository.getByPermissionId(permissionId)).thenReturn(request);

        // Mock DataNeedService returning a DIFFERENT type (e.g. Accounting Point)
        DataNeedInterface accountingNeed = new AccountingPointDataNeed();
        when(dataNeedsService.getById(dataNeedId)).thenReturn(accountingNeed);

        // --- ACT ---
        handler.accept(event);

        // --- ASSERT ---
        // Verify we NEVER called the API or Stream
        verify(apiClient, never()).getValidatedHistoricalData(any());
        verify(stream, never()).publish(any());
        verify(trackingRepository, never()).save(any());
    }
}