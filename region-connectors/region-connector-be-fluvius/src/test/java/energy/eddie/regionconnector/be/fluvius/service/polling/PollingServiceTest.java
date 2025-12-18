package energy.eddie.regionconnector.be.fluvius.service.polling;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.DataServiceType;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApiClient;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    @Mock
    private FluviusApiClient apiClient;
    @Mock
    private IdentifiableDataStreams streams;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private PollingService service;

    @Test
    void isActiveAndNeedsToBeFetcher_notStarted_returnsFalse() {
        // Given
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .start(LocalDate.now(ZoneOffset.UTC).plusDays(1))
                                                       .build();

        // When
        var res = service.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertFalse(res);
    }

    @Test
    void isActiveAndNeedsToBeFetcher_alreadyFetched_returnsFalse() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .start(now.minusDays(1).toLocalDate())
                                                       .addMeterReadings(new MeterReading("pid", "mid", now))
                                                       .build();

        // When
        var res = service.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertFalse(res);
    }

    @Test
    void isActiveAndNeedsToBeFetcher_forActivePermissionRequest_returnsTrue() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .start(now.minusDays(1))
                                                       .build();

        // When
        var res = service.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertTrue(res);
    }

    @Test
    void isActiveAndNeedsToBeFetcher_forActiveFuturePermissionRequest_returnsTrue() {
        // Given
        var yesterday = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .start(yesterday.toLocalDate())
                                                       .addMeterReadings(new MeterReading("pid", "mid", yesterday))
                                                       .build();

        // When
        var res = service.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertTrue(res);
    }

    @Test
    void testPollEnergyData_correctStartAndEnd() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .start(now.minusDays(2))
                                                       .addMeterReadings(new MeterReading("pid", "ean", null))
                                                       .dataNeedId("did")
                                                       .build();
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.pollTimeSeriesData(pr);

        // Then
        verify(apiClient)
                .energy(
                        eq("pid"),
                        any(),
                        eq(DataServiceType.QUARTER_HOURLY),
                        eq(ZonedDateTime.of(now.minusDays(2).atStartOfDay(), ZoneOffset.UTC)),
                        eq(now.atStartOfDay(ZoneOffset.UTC))
                );
    }

    @Test
    void testPollEnergyData_15minGranularity_callsApiClient() {
        // Given
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .granularity(Granularity.PT15M)
                                                       .addMeterReadings(new MeterReading("pid", "ean", null))
                                                       .dataNeedId("did")
                                                       .build();
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.pollTimeSeriesData(pr);

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(DataServiceType.QUARTER_HOURLY), any(), any());
    }

    @ParameterizedTest
    @MethodSource("testPollEnergyData_retriesOnError")
    void testPollEnergyData_retriesOnError(Exception error) {
        // Given
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .granularity(Granularity.PT15M)
                                                       .addMeterReadings(new MeterReading("pid", "ean", null))
                                                       .dataNeedId("did")
                                                       .build();
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.error(error))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.pollTimeSeriesData(pr);

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(DataServiceType.QUARTER_HOURLY), any(), any());
    }

    @Test
    void testPollEnergyData_1dayGranularity_callsApiClient() {
        // Given
        var pr = DefaultFluviusPermissionRequestBuilder
                .create()
                .dataNeedId("did")
                .granularity(Granularity.P1D)
                .addMeterReadings(new MeterReading("pid", "ean", null))
                .build();
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.DAILY), any(), any()))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        service.pollTimeSeriesData(pr);

        // Then
        verify(apiClient).energy(eq("pid"), any(), eq(DataServiceType.DAILY), any(), any());
    }

    @Test
    void testPollEnergyData_responseWithData_emitMeteringData() {
        // Given
        var permissionRequest = DefaultFluviusPermissionRequestBuilder
                .create()
                .dataNeedId("did")
                .granularity(Granularity.P1D)
                .addMeterReadings(new MeterReading("pid", "ean", null))
                .build();
        var sampleEnergyResponseModels = createSampleGetEnergyResponseModels();
        when(apiClient.energy(any(), any(), any(), any(), any())).thenReturn(Mono.just(sampleEnergyResponseModels));

        // When
        service.pollTimeSeriesData(permissionRequest);

        // Then
        verify(streams).publish(permissionRequest, sampleEnergyResponseModels);
    }

    @Test
    void testPollEnergyData_responseWithoutData_nothingIsEmitted() {
        // Given
        var permissionRequest = DefaultFluviusPermissionRequestBuilder
                .create()
                .dataNeedId("did")
                .granularity(Granularity.P1D)
                .addMeterReadings(new MeterReading("pid", "ean", null))
                .build();
        when(apiClient.energy(any(), any(), any(), any(), any()))
                .thenReturn(
                        Mono.just(
                                new GetEnergyResponseModelApiDataResponse(
                                        null,
                                        new GetEnergyResponseModel(null, null, null)
                                )
                        )
                );

        // When
        service.pollTimeSeriesData(permissionRequest);

        // Then
        verify(streams, never()).publish(permissionRequest, new GetEnergyResponseModelApiDataResponse());
    }

    @Test
    void testPollEnergyData_revokesPermissionOnForbidden() {
        // Given
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .granularity(Granularity.PT15M)
                                                       .addMeterReadings(new MeterReading("pid", "ean", null))
                                                       .dataNeedId("did")
                                                       .build();
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)));

        // When
        service.pollTimeSeriesData(pr);

        // Then
        verify(outbox).commit(assertArg(res -> assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REVOKED, res.status())
        )));
    }

    @Test
    void testPollEnergyData_onUnknownException_doesNothing() {
        // Given
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .granularity(Granularity.PT15M)
                                                       .addMeterReadings(new MeterReading("pid", "ean", null))
                                                       .dataNeedId("did")
                                                       .build();
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        // When
        service.pollTimeSeriesData(pr);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testPollEnergyData_forUnrelatedError_doesNotRevoke() {
        // Given
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .granularity(Granularity.PT15M)
                                                       .addMeterReadings(new MeterReading("pid", "ean", null))
                                                       .dataNeedId("did")
                                                       .build();
        when(apiClient.energy(eq("pid"), eq("ean"), eq(DataServiceType.QUARTER_HOURLY), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)));

        // When
        service.pollTimeSeriesData(pr);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testForcePoll_pollsHistoricalValidatedData() {
        // Given
        var lastMeterReadingDate = ZonedDateTime.of(2025, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var from = LocalDate.of(2024, 2, 1);
        var to = LocalDate.of(2024, 3, 1);
        var request = DefaultFluviusPermissionRequestBuilder.create()
                                                            .granularity(Granularity.PT15M)
                                                            .addMeterReadings(new MeterReading("pid",
                                                                                               "ean",
                                                                                               lastMeterReadingDate))
                                                            .dataNeedId("did")
                                                            .start(LocalDate.of(2025, 1, 1))
                                                            .start(LocalDate.of(2025, 4, 1))
                                                            .build();
        when(apiClient.energy(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(createSampleGetEnergyResponseModels()));

        // When
        var res = service.forcePoll(request, from, to);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(apiClient).energy("pid",
                                 "ean",
                                 DataServiceType.QUARTER_HOURLY,
                                 from.atStartOfDay(ZoneOffset.UTC),
                                 endOfDay(to, ZoneOffset.UTC));
    }

    private static Stream<Arguments> testPollEnergyData_retriesOnError() {
        return Stream.of(
                Arguments.of(WebClientResponseException.create(HttpStatus.TOO_MANY_REQUESTS.value(),
                                                               "",
                                                               null,
                                                               null,
                                                               null)),
                Arguments.of(WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(),
                                                               "",
                                                               null,
                                                               null,
                                                               null))
        );
    }

    private GetEnergyResponseModelApiDataResponse createSampleGetEnergyResponseModels() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        return new GetEnergyResponseModelApiDataResponse(
                null,
                new GetEnergyResponseModel(null, null, List.of(
                        new ElectricityMeterResponseModel(
                                1,
                                "meterId",
                                List.of(
                                        new EDailyEnergyItemResponseModel(
                                                now.minusMinutes(15),
                                                now,
                                                List.of(

                                                        new EMeasurementItemResponseModel(
                                                                "kwh",
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                5.0,
                                                                null,
                                                                null,
                                                                null
                                                        )
                                                )
                                        )
                                ),
                                null
                        )
                ))
        );
    }
}