package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.PeriodtimeInterval;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.EnerginetCustomerAcceptedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    @Mock
    private EnerginetCustomerApi customerApi;
    @Mock
    private PermissionRequestService permissionRequestService;
    private PollingService pollingService;


    @BeforeEach
    void setUp() {
        pollingService = new PollingService(customerApi, permissionRequestService);
    }

    @Test
    void fetchingAConsumptionRecord_revokesPermissionRequest_whenTokenInvalid() {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId, start, end,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);

        StateBuilderFactory factory = new StateBuilderFactory();
        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi,
                factory
        );
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "", HttpHeaders.EMPTY, null, null);
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest, factory));
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                .then(pollingService::close)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());
    }

    @Test
    void fetchingAConsumptionRecord_throwsPastStateExceptionOnWrongState() {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId, start, end,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi,
                new StateBuilderFactory()
        );
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "", HttpHeaders.EMPTY, null, null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                .then(pollingService::close)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
        assertEquals(PermissionProcessStatus.CREATED, permissionRequest.state().status());
    }

    @Test
    void fetchingAConsumptionRecord_doesNotRevokePermission_on5xx() {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId, start, end,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);

        StateBuilderFactory factory = new StateBuilderFactory();
        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi,
                factory
        );
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", HttpHeaders.EMPTY, null, null);
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest, factory));
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                .then(pollingService::close)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
        assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.state().status());
    }

    @Test
    void fetchingAConsumptionRecord_emitsRecord() {
        // Given
        var start = LocalDate.now().atStartOfDay(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId, start, end,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);

        StateBuilderFactory factory = new StateBuilderFactory();
        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi,
                factory
        );
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest, factory));
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        MyEnergyDataMarketDocumentResponse resultItem = new MyEnergyDataMarketDocumentResponse();
        resultItem.setMyEnergyDataMarketDocument(new MyEnergyDataMarketDocument()
                .periodTimeInterval(new PeriodtimeInterval()
                        .start(start.withZoneSameInstant(ZoneOffset.UTC).toString())
                        .end(end.withZoneSameInstant(ZoneOffset.UTC).toString()))
        );
        MyEnergyDataMarketDocumentResponseListApiResponse data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(resultItem);
        when(customerApi.getTimeSeries(eq(start.toLocalDate()), eq(end.toLocalDate()), any(), any(), eq("token"), any()))
                .thenReturn(Mono.just(data));

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                .assertNext(mr -> assertAll(
                        () -> assertEquals(permissionRequest.permissionId(), mr.permissionId()),
                        () -> assertEquals(permissionRequest.connectionId(), mr.connectionId()),
                        () -> assertEquals(permissionRequest.dataNeedId(), mr.dataNeedId()),
                        () -> assertNotNull(mr.apiResponse()),
                        () -> assertNotEquals(permissionRequest.start(), permissionRequest.lastPolled())
                ))
                .then(pollingService::close)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void fetchingInactiveConsumptionRecord_doesNotEmit() {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).plusDays(1);
        var end = start.plusDays(5);
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId, start, end,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);

        StateBuilderFactory factory = new StateBuilderFactory();
        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi,
                factory
        );
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest, factory));

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(pollingService::close)
                .verifyComplete();
    }

    @Test
    void fetchFutureMeterReadings_emitsRecords() {
        // Given
        var start1 = ZonedDateTime.now(DK_ZONE_ID).minusDays(1);
        var end1 = start1.plusDays(5);
        var start2 = ZonedDateTime.now(DK_ZONE_ID).plusDays(1);
        var end2 = start2.plusDays(5);
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation1 = new PermissionRequestForCreation("connId1", start1, end1,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);
        StateBuilderFactory factory = new StateBuilderFactory();
        DkEnerginetCustomerPermissionRequest permissionRequest1 = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation1,
                customerApi,
                factory
        );
        permissionRequest1.changeState(new EnerginetCustomerAcceptedState(permissionRequest1, factory));
        MyEnergyDataMarketDocumentResponse resultItem = new MyEnergyDataMarketDocumentResponse();
        resultItem.setMyEnergyDataMarketDocument(new MyEnergyDataMarketDocument()
                .periodTimeInterval(new PeriodtimeInterval()
                        .start(start1.withZoneSameInstant(ZoneOffset.UTC).toString())
                        .end(end1.withZoneSameInstant(ZoneOffset.UTC).toString()))
        );
        MyEnergyDataMarketDocumentResponseListApiResponse data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(resultItem);
        when(customerApi.getTimeSeries(eq(start1.toLocalDate()), any(), any(), any(), eq("token"), any()))
                .thenReturn(Mono.just(data));

        PermissionRequestForCreation requestForCreation2 = new PermissionRequestForCreation("connId2", start2, end2,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);
        DkEnerginetCustomerPermissionRequest permissionRequest2 = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation2,
                customerApi,
                factory
        );
        permissionRequest2.changeState(new EnerginetCustomerAcceptedState(permissionRequest2, factory));
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());

        when(permissionRequestService.findAllAcceptedPermissionRequests())
                .thenReturn(List.of(permissionRequest1, permissionRequest2));

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(() -> pollingService.fetchFutureMeterReadings())
                .then(pollingService::close)
                .assertNext(mr -> assertAll(
                        () -> assertEquals(permissionRequest1.permissionId(), mr.permissionId()),
                        () -> assertEquals(permissionRequest1.connectionId(), mr.connectionId()),
                        () -> assertEquals(permissionRequest1.dataNeedId(), mr.dataNeedId()),
                        () -> assertNotNull(mr.apiResponse()),
                        () -> assertNotEquals(permissionRequest1.start(), permissionRequest1.lastPolled())
                ))
                .verifyComplete();
    }
}