// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MeterReadingEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnergyDataServiceTest {
    @SuppressWarnings("unused")
    @Spy
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock
    private Outbox outbox;
    @InjectMocks
    private EnergyDataService energyDataService;
    @Captor
    private ArgumentCaptor<MeterReadingEvent> eventCaptor;

    @Test
    void publishEmitsNewLatestMeterReading() {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(Map.of())
                                                      .build();
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var end = ZonedDateTime.parse("2024-07-27T23:00:00Z");

        // When
        energyDataService.publish(List.of(resp), pr);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        assertEquals(Map.of("642502030419633983", end), eventCaptor.getValue().lastMeterReadings());
    }

    @Test
    void publishDoesNotOverrideNewerMeterReading() {
        // Given
        var end = ZonedDateTime.parse("2024-07-27T23:00:00Z");
        var meterId = "642502030419633983";
        var oldEnd = end.plusDays(1);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(Map.of(meterId, oldEnd))
                                                      .build();
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);

        // When
        energyDataService.publish(List.of(resp), pr);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        assertEquals(Map.of(meterId, oldEnd), eventCaptor.getValue().lastMeterReadings());
    }

    @Test
    void publishEmitsNoMeterReadingEvent_whenResponseEmpty() {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(null)
                                                      .build();
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_ERRORS);

        // When
        energyDataService.publish(List.of(resp), pr);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void publishEmitsRawData() {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(Map.of())
                                                      .build();
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);

        // When
        energyDataService.publish(List.of(resp), pr);

        // Then
        StepVerifier.create(energyDataService.getRawDataStream())
                    .expectNextCount(1)
                    .then(() -> energyDataService.close())
                    .verifyComplete();
    }
}