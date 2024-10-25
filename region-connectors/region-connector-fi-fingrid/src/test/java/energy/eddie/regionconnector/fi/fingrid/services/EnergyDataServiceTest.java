package energy.eddie.regionconnector.fi.fingrid.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MeterReadingEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnergyDataServiceTest {
    @SuppressWarnings("unused")
    @Spy
    private final ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    @Mock
    private Outbox outbox;
    @InjectMocks
    private EnergyDataService energyDataService;
    @Captor
    private ArgumentCaptor<MeterReadingEvent> eventCaptor;

    @Test
    void publishEmitsNewLatestMeterReading() throws IOException {
        // Given
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                "cid",
                "mid",
                Granularity.PT15M,
                null
        );
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var end = ZonedDateTime.parse("2024-07-27T23:00:00Z");

        // When
        energyDataService.publish(resp, pr);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        assertEquals(end, eventCaptor.getValue().latestMeterReading());
    }

    @Test
    void publishEmitsNoMeterReadingEvent_whenResponseEmpty() throws IOException {
        // Given
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                "cid",
                "mid",
                Granularity.PT15M,
                null
        );
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_ERRORS);

        // When
        energyDataService.publish(resp, pr);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void publishEmitsRawData() throws IOException {
        // Given
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                "cid",
                "mid",
                Granularity.PT15M,
                null
        );
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);

        // When
        energyDataService.publish(resp, pr);

        // Then
        StepVerifier.create(energyDataService.getRawDataStream())
                    .expectNextCount(1)
                    .then(() -> energyDataService.close())
                    .verifyComplete();
    }

    @Test
    void publishEmitsVHD() throws IOException {
        // Given
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                "cid",
                "mid",
                Granularity.PT15M,
                null
        );
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);

        // When
        energyDataService.publish(resp, pr);

        // Then
        StepVerifier.create(energyDataService.getValidatedHistoricalDataMarketDocumentsStream())
                    .expectNextCount(1)
                    .then(() -> energyDataService.close())
                    .verifyComplete();
    }
}