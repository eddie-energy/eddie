package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.api.UnauthorizedException;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.adapter.JdkFlowAdapter;
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
    public static Stream<Arguments> onApiCallThrowsOtherException_doesNotRevokePermissionRequest() {
        return Stream.of(
                Arguments.of(new NoSuppliesException("Other")),
                Arguments.of(new InvalidPointAndMeasurementTypeCombinationException(0, MeasurementType.QUARTER_HOURLY)),
                Arguments.of(new RuntimeException(new NoSuppliesException("Other")))
        );
    }

    public static Stream<Arguments> onApiCallThrowsUnauthorized_revokesPermissionRequest() {
        return Stream.of(
                Arguments.of(new UnauthorizedException("Unauthorized")),
                Arguments.of(new RuntimeException(new UnauthorizedException("Unauthorized")))
        );
    }

    @Test
    void close_emitsCompleteOnPublisher() {
        // Given
        var scheduler = new DatadisScheduler(mock(DataApi.class), Sinks.many().multicast().onBackpressureBuffer());

        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(scheduler.getConsumptionRecordStream()))
                .expectComplete()
                .verifyLater();

        // When
        scheduler.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @ParameterizedTest
    @MethodSource
    void onApiCallThrowsUnauthorized_revokesPermissionRequest(Exception exception) {
        // Given
        var dataApi = mock(DataApi.class);
        doReturn(Mono.error(exception))
                .when(dataApi).getSupplies(anyString(), isNull());
        var scheduler = new DatadisScheduler(dataApi, Sinks.many().multicast().onBackpressureBuffer());
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, MeasurementType.QUARTER_HOURLY);
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class), new InMemoryPermissionRequestRepository());
        permissionRequest.changeState(new AcceptedState(permissionRequest));

        // When
        scheduler.pullAvailableHistoricalData(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());

        // Clean-Up
        scheduler.close();
    }

    @Test
    void onLaterApiCallThrowsUnauthorized_revokesPermissionRequest() {
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
                "distCode"
        );
        doReturn(Mono.just(List.of(supply)))
                .when(dataApi).getSupplies(anyString(), isNull());
        doReturn(Mono.error(new UnauthorizedException("Unauthorized")))
                .when(dataApi).getConsumptionKwh(any());
        var scheduler = new DatadisScheduler(dataApi, Sinks.many().multicast().onBackpressureBuffer());

        var creation = new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, MeasurementType.QUARTER_HOURLY);
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class), new InMemoryPermissionRequestRepository());
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
    void onApiCallThrowsUnauthorized_doesNotRevokeWhenPermissionRequestNotAccepted() {
        // Given
        var dataApi = mock(DataApi.class);
        doReturn(Mono.error(new UnauthorizedException("Unauthorized")))
                .when(dataApi).getSupplies(anyString(), isNull());
        var scheduler = new DatadisScheduler(dataApi, Sinks.many().multicast().onBackpressureBuffer());
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, MeasurementType.QUARTER_HOURLY);
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class), new InMemoryPermissionRequestRepository());

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
        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", creation, mock(AuthorizationApi.class), new InMemoryPermissionRequestRepository());
        permissionRequest.changeState(new AcceptedState(permissionRequest));

        // When
        scheduler.pullAvailableHistoricalData(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.state().status());

        // Clean-Up
        scheduler.close();
    }
}