package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.PointType;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.permission.events.EsInternalPollingEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class DataApiServiceTest {
    private final Sinks.Many<IdentifiableMeteringData> meteringDataSink = Sinks.many()
                                                                               .multicast()
                                                                               .onBackpressureBuffer();
    @Mock
    private DataApi dataApi;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<PermissionEvent> eventCaptor;
    private MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService;
    private final String timeZone = "Europe/Madrid";

    @BeforeEach
    @SuppressWarnings("DirectInvocationOnMock")
        // The outbox is a mock, but it is not directly called here, but it needs to be called by the UpdateAndFulfillmentService
    void setUp() {
        meterReadingPermissionUpdateAndFulfillmentService =
                new MeterReadingPermissionUpdateAndFulfillmentService(
                        new FulfillmentService(
                                outbox,
                                EsSimpleEvent::new
                        ),
                        (pr, last) -> outbox.commit(new EsInternalPollingEvent(pr.permissionId(), last)));
    }

    @Test
    void fetchDataForPermissionRequest_callsDataApi_withExpectedMeteringDataRequest() {
        // Given
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.empty());

        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService<>(dataApi,
                meteringDataSink,
                meterReadingPermissionUpdateAndFulfillmentService,
                outbox);

        // When
        dataApiService.pollTimeSeriesData(permissionRequest, timeZone);

        // Then
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        verifyNoMoreInteractions(dataApi);

        StepVerifier.create(meteringDataSink.asFlux())
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @Test
    void fetchDataForPermissionRequest_dataEndDateEqualPermissionEndDate_doesNotFulfillPermissionRequest() throws IOException {
        // Given
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData)
                                                               .block(Duration.ofMillis(10));
        assert intermediateMeteringData != null;
        LocalDate start = intermediateMeteringData.start();
        LocalDate end = intermediateMeteringData.end();
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end);

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);
        when(dataApi.getConsumptionKwh(expectedMeteringDataRequest)).thenReturn(Mono.just(meteringData));

        var dataApiService = new DataApiService<>(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService,
                                                outbox);

        // When
        dataApiService.pollTimeSeriesData(permissionRequest, timeZone);

        // Then
        StepVerifier.create(meteringDataSink.asFlux())
                    .expectNextCount(1)
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(isA(EsInternalPollingEvent.class));
    }

    @Test
    void fetchDataForPermissionRequest_dataEndDateAfterPermissionEndDate_fulfillsPermissionRequest() throws IOException {
        // Given
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData)
                                                               .block(Duration.ofMillis(10));
        assert intermediateMeteringData != null;
        LocalDate start = intermediateMeteringData.start();
        LocalDate end = intermediateMeteringData.end();
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end.minusDays(1));

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest,
                                                                                    start,
                                                                                    end);
        when(dataApi.getConsumptionKwh(expectedMeteringDataRequest)).thenReturn(Mono.just(meteringData));

        var dataApiService = new DataApiService<>(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService,
                                                outbox);

        // When
        dataApiService.pollTimeSeriesData(permissionRequest, timeZone);

        // Then
        StepVerifier.create(meteringDataSink.asFlux())
                    .expectNextCount(1)
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));

        verify(outbox, times(2)).commit(eventCaptor.capture());
        // First one is the polling event, we're not testing for that
        var second = eventCaptor.getAllValues().get(1);
        assertEquals(PermissionProcessStatus.FULFILLED, second.status());
    }

    @Test
    void fetchDataForPermissionRequest_dataEndDateBeforePermissionEndDate_doesNotFulfillPermissionRequest() throws IOException {
        // Given
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData)
                                                               .block(Duration.ofMillis(10));
        assert intermediateMeteringData != null;
        LocalDate start = intermediateMeteringData.start();
        LocalDate end = intermediateMeteringData.end();
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, end.plusDays(1));

        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);
        when(dataApi.getConsumptionKwh(expectedMeteringDataRequest)).thenReturn(Mono.just(meteringData));

        Sinks.Many<IdentifiableMeteringData> sink = Sinks.many().multicast().onBackpressureBuffer();
        var dataApiService = new DataApiService<>(dataApi,
                                                sink,
                                                meterReadingPermissionUpdateAndFulfillmentService,
                                                outbox);

        // When
        dataApiService.pollTimeSeriesData(permissionRequest, timeZone);

        // Then
        StepVerifier.create(sink.asFlux())
                    .expectNextCount(1)
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.ACCEPTED, event.status())));
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

        var dataApiService = new DataApiService<>(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService,
                                                outbox);

        // When
        dataApiService.pollTimeSeriesData(permissionRequest, timeZone);

        // Then
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        verifyNoMoreInteractions(dataApi);

        StepVerifier.create(meteringDataSink.asFlux())
                    .then(dataApiService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
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

        var dataApiService = new DataApiService<>(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService,
                                                outbox);

        // When
        dataApiService.pollTimeSeriesData(permissionRequest, timeZone);

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

        var dataApiService = new DataApiService<>(dataApi,
                                                meteringDataSink,
                                                meterReadingPermissionUpdateAndFulfillmentService,
                                                outbox);
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var expectedNrOfRetries = ChronoUnit.MONTHS.between(now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST), start);
        // When
        dataApiService.pollTimeSeriesData(permissionRequest, timeZone);

        // Then
        verify(dataApi,
               atLeast((int) expectedNrOfRetries)).getConsumptionKwh(captor.capture()); // atLeast because 29 of feb can be a problem
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

    private static EsPermissionRequest acceptedPermissionRequest(LocalDate start, LocalDate end) {
        return new DatadisPermissionRequestBuilder()
                .setGranularity(Granularity.PT1H)
                .setStart(start)
                .setEnd(end)
                .setDistributorCode(DistributorCode.ASEME)
                .setPointType(PointType.TYPE_1)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .build();
    }
}
