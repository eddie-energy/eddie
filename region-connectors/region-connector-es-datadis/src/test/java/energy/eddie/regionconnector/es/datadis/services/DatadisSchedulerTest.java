package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatadisSchedulerTest {

    public static final Supply SUPPLY = new Supply(
            "Streetname",
            "mpid",
            "1234",
            "province",
            "mun",
            "dist",
            ZonedDateTime.now(ZoneOffset.UTC).minusDays(20).toLocalDate(),
            ZonedDateTime.now(ZoneOffset.UTC).minusDays(10).toLocalDate(),
            1,
            "1"
    );

    public static Stream<Arguments> onApiCallThrowsOtherException_doesNotRevokePermissionRequest() {
        return Stream.of(
                Arguments.of(new NoSuppliesException("Other")),
                Arguments.of(new InvalidPointAndMeasurementTypeCombinationException(0, MeasurementType.QUARTER_HOURLY)),
                Arguments.of(new RuntimeException(new NoSuppliesException("Other")))
        );
    }

    public static Stream<Arguments> onApiCallThrowsForbidden_revokesPermissionRequest() {
        return Stream.of(
                Arguments.of(new DatadisApiException("Forbidden", HttpResponseStatus.FORBIDDEN, "Forbidden")),
                Arguments.of(new RuntimeException(new DatadisApiException("Forbidden", HttpResponseStatus.FORBIDDEN, "Forbidden")))
        );
    }

    @Test
    void close_emitsCompleteOnPublisher() {
        // Given
        Sinks.Many<IdentifiableMeteringData> meteringDataSink = Sinks.many().multicast().onBackpressureBuffer();
        var scheduler = new DatadisScheduler(mock(DataApi.class), meteringDataSink);

        StepVerifier stepVerifier = StepVerifier.create(meteringDataSink.asFlux())
                .expectComplete()
                .verifyLater();

        // When
        scheduler.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @ParameterizedTest
    @MethodSource
    void onApiCallThrowsForbidden_revokesPermissionRequest(Exception exception) {
        // Given
        var dataApi = mock(DataApi.class);
        when(dataApi.getSupplies(anyString(), isNull()))
                .thenReturn(Mono.just(List.of(SUPPLY)));
        doReturn(Mono.error(exception))
                .when(dataApi).getConsumptionKwh(any());
        var scheduler = new DatadisScheduler(dataApi, Sinks.many().multicast().onBackpressureBuffer());
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        var end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, MeasurementType.QUARTER_HOURLY);
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class));
        permissionRequest.changeState(new AcceptedState(permissionRequest));

        // When
        scheduler.pullAvailableHistoricalData(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());

        // Clean-Up
        scheduler.close();
    }

    @Test
    void onLaterApiCallThrowsForbidden_revokesPermissionRequest() {
        // Given
        var dataApi = mock(DataApi.class);
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        var end = start.plusDays(10);
        Supply supply = new Supply(
                "Streetname",
                "mpid",
                "1234",
                "province",
                "mun",
                "dist",
                start.toLocalDate(),
                end.toLocalDate(),
                1,
                "1"
        );
        doReturn(Mono.just(List.of(supply)))
                .when(dataApi).getSupplies(anyString(), isNull());
        doReturn(Mono.error(new DatadisApiException("Forbidden", HttpResponseStatus.FORBIDDEN, "Forbidden")))
                .when(dataApi).getConsumptionKwh(any());
        var scheduler = new DatadisScheduler(dataApi, Sinks.many().multicast().onBackpressureBuffer());

        var creation = new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, MeasurementType.QUARTER_HOURLY);
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class));
        permissionRequest.changeState(new AcceptedState(permissionRequest));

        // When
        scheduler.pullAvailableHistoricalData(permissionRequest);

        // Then
        verify(dataApi, times(1)).getConsumptionKwh(any());
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());

        // Clean-Up
        scheduler.close();
    }

    @Test
    void onApiCallThrowsForbidden_doesNotRevokeWhenPermissionRequestNotAccepted() {
        // Given
        var dataApi = mock(DataApi.class);
        doReturn(Mono.error(new DatadisApiException("Forbidden", HttpResponseStatus.FORBIDDEN, "Forbidden")))
                .when(dataApi).getSupplies(anyString(), isNull());
        var scheduler = new DatadisScheduler(dataApi, Sinks.many().multicast().onBackpressureBuffer());
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, MeasurementType.QUARTER_HOURLY);
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class));

        // When
        scheduler.pullAvailableHistoricalData(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.CREATED, permissionRequest.state().status());

        // Clean-Up
        scheduler.close();
    }

    @ParameterizedTest
    @MethodSource
    void onApiCallThrowsOtherException_doesNotRevokePermissionRequest(Exception exception) {
        // Given
        var dataApi = mock(DataApi.class);

        doReturn(Mono.error(exception))
                .when(dataApi).getSupplies(anyString(), isNull());
        var scheduler = new DatadisScheduler(dataApi, Sinks.many().multicast().onBackpressureBuffer());
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, MeasurementType.QUARTER_HOURLY);
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class));
        permissionRequest.changeState(new AcceptedState(permissionRequest));

        // When
        scheduler.pullAvailableHistoricalData(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.state().status());

        // Clean-Up
        scheduler.close();
    }
}