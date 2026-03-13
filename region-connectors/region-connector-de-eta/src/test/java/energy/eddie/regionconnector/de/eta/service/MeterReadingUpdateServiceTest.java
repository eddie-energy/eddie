package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingUpdateServiceTest {

    @Mock
    private ValidatedHistoricalDataStream stream;

    @Mock
    private Outbox outbox;

    @Test
    @DisplayName("Should emit LatestMeterReadingEvent with correct permissionId and date when stream emits data")
    void shouldEmitLatestMeterReadingEventWhenStreamEmitsData() {
        String permissionId = "perm-123";
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .end(endDate)
                .build();

        EtaPlusMeteredData meteredData = new EtaPlusMeteredData(
                "malo-1",
                LocalDate.of(2024, 10, 1),
                endDate,
                List.of(),
                "{}"
        );

        IdentifiableValidatedHistoricalData identifiableData =
                new IdentifiableValidatedHistoricalData(permissionRequest, meteredData);

        when(stream.validatedHistoricalData()).thenReturn(Flux.just(identifiableData));

        new MeterReadingUpdateService(stream, outbox);

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(eventCaptor.capture());

        PermissionEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isInstanceOf(LatestMeterReadingEvent.class);

        LatestMeterReadingEvent latestMeterReadingEvent = (LatestMeterReadingEvent) capturedEvent;
        assertThat(latestMeterReadingEvent.permissionId()).isEqualTo(permissionId);
        assertThat(latestMeterReadingEvent.latestMeterReading()).isEqualTo(endDate);
    }
}
