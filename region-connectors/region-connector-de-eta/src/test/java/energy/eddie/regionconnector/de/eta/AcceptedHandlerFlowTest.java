package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.handlers.AcceptedHandler;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;
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
                outbox
        );
    }

    @Test
    @DisplayName("Should fetch data and emit LatestMeterReadingEvent when AcceptedEvent is received")
    void shouldFetchDataAndEmitLatestReadingEvent() {
        String permissionId = "perm-123";
        LocalDate start = LocalDate.now(ZoneId.of("UTC")).minusMonths(3);
        LocalDate end = LocalDate.now(ZoneId.of("UTC")).minusDays(1);

        DePermissionRequest mockRequest = new DePermissionRequest(
                permissionId,
                "conn-1",
                "malo-1",
                start,
                end,
                Granularity.PT15M,
                EnergyType.ELECTRICITY,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneId.of("UTC")),
                "need-1",
                null,
                null,
                null
        );

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
                List.of(reading),
                "{}"
        );

        when(apiClient.fetchMeteredData(mockRequest)).thenReturn(Mono.just(mockApiData));

        AcceptedEvent event = new AcceptedEvent(permissionId);
        acceptedHandler.accept(event);

        verify(apiClient).fetchMeteredData(mockRequest);

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);

        verify(outbox, times(1)).commit(eventCaptor.capture());

        Object capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isInstanceOf(LatestMeterReadingEvent.class);

        LatestMeterReadingEvent readingEvent = (LatestMeterReadingEvent) capturedEvent;
        assertThat(readingEvent.permissionId()).isEqualTo(permissionId);

        assertThat(readingEvent.latestMeterReading()).isEqualTo(end);
    }
}