package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.services.StateFulfillmentService;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
class DataApiServiceTest {
    private final DataApi dataApi = mock(DataApi.class);
    private final MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService =
            new MeterReadingPermissionUpdateAndFulfillmentService(new StateFulfillmentService());
    private final Sinks.Many<IdentifiableMeteringData> meteringDataSink = Sinks.many()
                                                                               .multicast()
                                                                               .onBackpressureBuffer();

    private static Stream<Arguments> variousTimeRanges() {
        LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
        return Stream.of(
                Arguments.of(now, now.plusDays(1), "1 day"),
                Arguments.of(now.minusMonths(1), now, "Last month"),
                Arguments.of(now.minusMonths(2), now.minusMonths(1), "1 month: 2 months ago"),
                Arguments.of(now.minusYears(1), now, "1 year: 12 months ago"),
                Arguments.of(now.minusMonths(20), now.minusMonths(19), "1 month: 20 months ago")
        );
    }

    @Test
    void fetchDataForPermissionRequest_callsDataApi_withExpectedMeteringDataRequest() {
        // Given
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.empty());

        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService);

        // When
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Then
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        verifyNoMoreInteractions(dataApi);

        StepVerifier.create(meteringDataSink.asFlux())
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    private static EsPermissionRequest acceptedPermissionRequest(LocalDate start, LocalDate end) {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId");
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId",
                                                                             permissionRequestForCreation,
                                                                             start,
                                                                             end,
                                                                             Granularity.PT1H,
                                                                             stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED)
                                                         .build());
        permissionRequest.setDistributorCodePointTypeAndProductionSupport(DistributorCode.ASEME, 1, false);
        return permissionRequest;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void fetchDataForPermissionRequest_dataEndDateEqualPermissionEndDate_doesNotFulfillPermissionRequest() throws IOException {
        // Given
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData);
        LocalDate start = intermediateMeteringData.start();
        LocalDate end = intermediateMeteringData.end();
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);
        when(dataApi.getConsumptionKwh(expectedMeteringDataRequest)).thenReturn(Mono.just(meteringData));

        var dataApiService = new DataApiService(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService);

        // When
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Then
        StepVerifier.create(meteringDataSink.asFlux())
                    .assertNext(identifiableMeteringData -> assertAll(
                            () -> assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.status()),
                            () -> assertEquals(intermediateMeteringData.end(),
                                               permissionRequest.latestMeterReadingEndDate().get())
                    ))
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void fetchDataForPermissionRequest_dataEndDateAfterPermissionEndDate_fulfillsPermissionRequest() throws IOException {
        // Given
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData);
        LocalDate start = intermediateMeteringData.start();
        LocalDate end = intermediateMeteringData.end();
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end.minusDays(1));

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest,
                                                                                    start,
                                                                                    end);
        when(dataApi.getConsumptionKwh(expectedMeteringDataRequest)).thenReturn(Mono.just(meteringData));

        var dataApiService = new DataApiService(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService);

        // When
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Then
        StepVerifier.create(meteringDataSink.asFlux())
                    .assertNext(identifiableMeteringData -> assertAll(
                            () -> assertEquals(PermissionProcessStatus.FULFILLED, permissionRequest.status()),
                            () -> assertEquals(end, permissionRequest.latestMeterReadingEndDate().get())
                    ))
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void fetchDataForPermissionRequest_dataEndDateBeforePermissionEndDate_doesNotFulfillPermissionRequest() throws IOException {
        // Given
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData);
        LocalDate start = intermediateMeteringData.start();
        LocalDate end = intermediateMeteringData.end();
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end.plusDays(1));

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);
        when(dataApi.getConsumptionKwh(expectedMeteringDataRequest)).thenReturn(Mono.just(meteringData));

        Sinks.Many<IdentifiableMeteringData> sink = Sinks.many().multicast().onBackpressureBuffer();
        var dataApiService = new DataApiService(dataApi,
                                                sink,
                                                meterReadingPermissionUpdateAndFulfillmentService);

        // When
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Then
        StepVerifier.create(sink.asFlux())
                    .assertNext(identifiableMeteringData -> assertAll(
                            () -> assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.status()),
                            () -> assertEquals(intermediateMeteringData.end(),
                                               permissionRequest.latestMeterReadingEndDate().get())
                    ))
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }


    @Test
    void fetchDataForPermissionRequest_dataApiReturnsForbidden_revokesPermission() {
        // Given
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(new DatadisApiException("",
                                                                                             HttpResponseStatus.FORBIDDEN,
                                                                                             "")));

        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);
        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService);

        // When
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Then
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.status());
        verifyNoMoreInteractions(dataApi);

        StepVerifier.create(meteringDataSink.asFlux())
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @Test
    void fetchDataForPermissionRequest_dataApiReturnsUnexpectedError_doesNothing() {
        // Given
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(new RuntimeException(new RuntimeException(
                "Unexpected error"))));

        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);
        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService);

        // When
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Then
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        verifyNoMoreInteractions(dataApi);

        StepVerifier.create(meteringDataSink.asFlux())
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }


    @ParameterizedTest(name = "{2}")
    @MethodSource("variousTimeRanges")
    void fetchDataForPermissionRequest_retries_withUpdatedMeteringDataRequest(
            LocalDate start,
            LocalDate end,
            String description
    ) {
        // Given
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(new DatadisApiException("",
                                                                                             HttpResponseStatus.TOO_MANY_REQUESTS,
                                                                                             "")));

        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);

        ArgumentCaptor<MeteringDataRequest> captor = ArgumentCaptor.forClass(MeteringDataRequest.class);
        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService);
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var expectedNrOfRetries = ChronoUnit.MONTHS.between(now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST), start);
        // When
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Then
        verify(dataApi, times((int) expectedNrOfRetries + 1)).getConsumptionKwh(captor.capture());
        // Check that each captured MeteringDataRequest is as expected
        for (MeteringDataRequest capturedRequest : captor.getAllValues()) {
            assertEquals(expectedMeteringDataRequest, capturedRequest);
            // Update expectedStart for the next iteration
            expectedMeteringDataRequest = expectedMeteringDataRequest.minusMonths(1);
        }
        verifyNoMoreInteractions(dataApi);

        StepVerifier.create(meteringDataSink.asFlux())
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }
}
