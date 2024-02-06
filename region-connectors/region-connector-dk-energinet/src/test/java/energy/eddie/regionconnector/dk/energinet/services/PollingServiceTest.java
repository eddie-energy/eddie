package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.EnerginetCustomerAcceptedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi
        );
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", HttpHeaders.EMPTY, null, null);
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest));
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(pollingService::close)
                .expectError(HttpClientErrorException.Unauthorized.class)
                .verify();
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
                customerApi
        );
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", HttpHeaders.EMPTY, null, null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(pollingService::close)
                .expectError(HttpClientErrorException.Unauthorized.class)
                .verify();
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

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi
        );
        HttpClientErrorException unauthorized = HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "", HttpHeaders.EMPTY, null, null);
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest));
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(pollingService::close)
                .expectError(HttpClientErrorException.class)
                .verify();
        assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.state().status());
    }

    @Test
    void fetchingAConsumptionRecord_emitsRecord() {
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
                customerApi
        );
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest));
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        MyEnergyDataMarketDocumentResponseListApiResponse data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(new MyEnergyDataMarketDocumentResponse());
        when(customerApi.getTimeSeries(eq(start), eq(end), any(), any(), eq("token"), any()))
                .thenReturn(Mono.just(data));

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(pollingService::close)
                .assertNext(mr -> assertAll(
                        () -> assertEquals(permissionRequest.permissionId(), mr.permissionId()),
                        () -> assertEquals(permissionRequest.connectionId(), mr.connectionId()),
                        () -> assertEquals(permissionRequest.dataNeedId(), mr.dataNeedId()),
                        () -> assertNotNull(mr.apiResponse()),
                        () -> assertNotEquals(permissionRequest.start(), permissionRequest.lastPolled())
                ))
                .verifyComplete();
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

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                customerApi
        );
        permissionRequest.changeState(new EnerginetCustomerAcceptedState(permissionRequest));

        // When
        pollingService.fetchHistoricalMeterReadings(permissionRequest);

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                .then(pollingService::close)
                .verifyComplete();
    }

    @Test
    void emitActivePermissionRequests_emitsRecords() {
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
        DkEnerginetCustomerPermissionRequest permissionRequest1 = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation1,
                customerApi
        );
        permissionRequest1.changeState(new EnerginetCustomerAcceptedState(permissionRequest1));
        MyEnergyDataMarketDocumentResponseListApiResponse data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(new MyEnergyDataMarketDocumentResponse());
        when(customerApi.getTimeSeries(eq(start1), any(), any(), any(), eq("token"), any()))
                .thenReturn(Mono.just(data));

        PermissionRequestForCreation requestForCreation2 = new PermissionRequestForCreation("connId2", start2, end2,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);
        DkEnerginetCustomerPermissionRequest permissionRequest2 = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation2,
                customerApi
        );
        permissionRequest2.changeState(new EnerginetCustomerAcceptedState(permissionRequest2));
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());

        when(permissionRequestService.findAllAcceptedPermissionRequests())
                .thenReturn(List.of(permissionRequest1, permissionRequest2));

        // When
        pollingService.emitActivePermissionRequests();

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
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