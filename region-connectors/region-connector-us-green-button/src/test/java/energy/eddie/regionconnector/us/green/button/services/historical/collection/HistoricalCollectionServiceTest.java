// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services.historical.collection;

import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.HistoricalCollectionResponse;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReadingPk;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalCollectionServiceTest {
    @Mock
    private DataNeedMatcher dataNeedMatcher;
    @Mock
    private GreenButtonApi api;
    @Mock
    private MeterReadingRepository repository;
    @InjectMocks
    private HistoricalCollectionService historicalCollectionService;

    @Test
    void testPersistMetersForPermissionRequests_returnsMeterReadings() {
        // Given
        var pr = getPermissionRequest(List.of());
        var meterListing = getMeterListing("1111");
        when(api.fetchMeters(Pages.NO_SLURP, List.of("1111"), "company"))
                .thenReturn(Flux.just(meterListing));
        when(dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(meterListing)).thenReturn(meterListing.meters());
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // When
        var res = historicalCollectionService.persistMetersForPermissionRequest(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(meterReading -> assertAll(
                            () -> assertEquals("pid", meterReading.permissionId()),
                            () -> assertEquals("uid", meterReading.meterUid()),
                            () -> assertFalse(meterReading.isReadyToPoll()),
                            () -> assertNull(meterReading.lastMeterReading())
                    ))
                    .verifyComplete();
    }

    @Test
    void testPersistMetersForPermissionRequestsWithMeters_doesNotOverwriteMeters() {
        // Given
        var reading = new MeterReading("pid", "1111", null, PollingStatus.DATA_NOT_READY);
        when(repository.findById(any()))
                .thenReturn(Optional.of(reading));
        var pr = getPermissionRequest(List.of(reading));
        var meterListing = getMeterListing("1111");
        when(api.fetchMeters(Pages.NO_SLURP, List.of("1111"), "company"))
                .thenReturn(Flux.just(meterListing));
        when(dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(meterListing)).thenReturn(meterListing.meters());

        // When
        var res = historicalCollectionService.persistMetersForPermissionRequest(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(meterReading -> assertAll(
                            () -> assertEquals("pid", meterReading.permissionId()),
                            () -> assertEquals("1111", meterReading.meterUid()),
                            () -> assertFalse(meterReading.isReadyToPoll()),
                            () -> assertNull(meterReading.lastMeterReading())
                    ))
                    .verifyComplete();
        verify(repository, never()).save(any());
    }

    @Test
    void testPersistMetersForPermissionRequests_filtersUnknownMeters() {
        // Given
        var pr = getPermissionRequest(List.of());
        var meterListing = getMeterListing("2222");
        when(api.fetchMeters(Pages.NO_SLURP, List.of("1111"), "company"))
                .thenReturn(Flux.just(meterListing));
        when(dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(meterListing)).thenReturn(meterListing.meters());

        // When
        var res = historicalCollectionService.persistMetersForPermissionRequest(pr);

        // Then
        StepVerifier.create(res)
                    .verifyComplete();
    }

    @Test
    void testTriggerHistoricalDataCollection_removesNotActivatedMeters() {
        // Given
        var meter1 = new MeterReading("pid", "uid1", null, PollingStatus.DATA_NOT_READY);
        var meter2 = new MeterReading("pid", "uid2", null, PollingStatus.DATA_NOT_READY);
        var permissionRequest = getPermissionRequest(List.of(meter1, meter2));
        when(api.collectHistoricalData(anyList(), anyString()))
                .thenReturn(Mono.just(new HistoricalCollectionResponse(true, List.of("uid1"))));

        // When
        var res = historicalCollectionService.triggerHistoricalDataCollection(permissionRequest);

        // Then
        StepVerifier.create(res)
                    .then(() -> verify(repository)
                            .deleteAllById(List.of(new MeterReadingPk("pid", "uid2"))))
                    .verifyComplete();
    }

    private static MeterListing getMeterListing(String authUid) {
        return new MeterListing(List.of(new Meter(
                "uid",
                authUid,
                null,
                null,
                null,
                false,
                false,
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(), List.of())), null);
    }

    private static GreenButtonPermissionRequest getPermissionRequest(List<@NotNull MeterReading> meterReadings) {
        return new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                        .setCompanyId("company")
                                                        .setLastMeterReadings(meterReadings)
                                                        .setAuthUid("1111")
                                                        .build();
    }
}