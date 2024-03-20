package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"resource", "unchecked"})
class DataApiServiceTest {

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
        // Arrange
        DataApi dataApi = mock(DataApi.class);
        Sinks.Many<IdentifiableMeteringData> meteringDataSink = mock(Sinks.Many.class);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.empty());

        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.distributorCode()).thenReturn(Optional.of(DistributorCode.ASEME));
        when(permissionRequest.pointType()).thenReturn(Optional.of(1));
        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi, meteringDataSink);

        // Act
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Assert
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        verifyNoMoreInteractions(dataApi);

        verifyNoInteractions(meteringDataSink);
    }

    @Test
    void fetchDataForPermissionRequest_dataApiReturnsForbidden_revokesPermission() throws StateTransitionException {
        // Arrange
        DataApi dataApi = mock(DataApi.class);
        Sinks.Many<IdentifiableMeteringData> meteringDataSink = mock(Sinks.Many.class);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(new DatadisApiException("", HttpResponseStatus.FORBIDDEN, "")));

        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.distributorCode()).thenReturn(Optional.of(DistributorCode.ASEME));
        when(permissionRequest.pointType()).thenReturn(Optional.of(1));
        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi, meteringDataSink);

        // Act
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Assert
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        verify(permissionRequest).revoke();
        verifyNoMoreInteractions(dataApi);

        verifyNoInteractions(meteringDataSink);
    }

    @Test
    void fetchDataForPermissionRequest_dataApiReturnsUnexpectedError_doesNothing() {
        // Arrange
        DataApi dataApi = mock(DataApi.class);
        Sinks.Many<IdentifiableMeteringData> meteringDataSink = mock(Sinks.Many.class);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(new RuntimeException(new RuntimeException("Unexpected error"))));

        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.distributorCode()).thenReturn(Optional.of(DistributorCode.ASEME));
        when(permissionRequest.pointType()).thenReturn(Optional.of(1));
        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi, meteringDataSink);

        // Act
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Assert
        verify(dataApi).getConsumptionKwh(expectedMeteringDataRequest);
        verifyNoMoreInteractions(dataApi);

        verifyNoInteractions(meteringDataSink);
    }


    @ParameterizedTest(name = "{2}")
    @MethodSource("variousTimeRanges")
    void fetchDataForPermissionRequest_retries_withUpdatedMeteringDataRequest(LocalDate start, LocalDate end, String description) {
        // Arrange
        DataApi dataApi = mock(DataApi.class);
        Sinks.Many<IdentifiableMeteringData> meteringDataSink = mock(Sinks.Many.class);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(new DatadisApiException("", HttpResponseStatus.TOO_MANY_REQUESTS, "")));

        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.distributorCode()).thenReturn(Optional.of(DistributorCode.ASEME));
        when(permissionRequest.pointType()).thenReturn(Optional.of(1));

        ArgumentCaptor<MeteringDataRequest> captor = ArgumentCaptor.forClass(MeteringDataRequest.class);
        var expectedMeteringDataRequest = MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end);

        var dataApiService = new DataApiService(dataApi, meteringDataSink);
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var expectedNrOfRetries = ChronoUnit.MONTHS.between(now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST), start);
        // Act
        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);

        // Assert
        verify(dataApi, times((int) expectedNrOfRetries + 1)).getConsumptionKwh(captor.capture());
        // Check that each captured MeteringDataRequest is as expected
        for (MeteringDataRequest capturedRequest : captor.getAllValues()) {
            assertEquals(expectedMeteringDataRequest, capturedRequest);
            // Update expectedStart for the next iteration
            expectedMeteringDataRequest = expectedMeteringDataRequest.minusMonths(1);
        }
        verifyNoMoreInteractions(dataApi);

        verifyNoInteractions(meteringDataSink);
    }

    @Test
    void close_emitsCompleteOnPublisher() {
        // Arrange
        Sinks.Many<IdentifiableMeteringData> meteringDataSink = Sinks.many().multicast().onBackpressureBuffer();
        var dataApiService = new DataApiService(mock(DataApi.class), meteringDataSink);

        StepVerifier stepVerifier = StepVerifier.create(meteringDataSink.asFlux())
                .expectComplete()
                .verifyLater();

        // Act
        dataApiService.close();

        // Assert
        stepVerifier.verify(Duration.ofSeconds(2));
    }
}