package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.DkEnerginetSpringConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.PeriodtimeInterval;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkInternalPollingEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.EventFulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    private final ObjectMapper mapper = new DkEnerginetSpringConfig().objectMapper();
    @Mock
    private EnerginetCustomerApi customerApi;
    @Mock
    private DkPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<DkInternalPollingEvent> pollingEventCaptor;
    private PollingService pollingService;

    @BeforeEach
    @SuppressWarnings("DirectInvocationOnMock")
        // the outbox is not directly invoked
    void setUp() {
        pollingService = new PollingService(
                customerApi,
                repository,
                new MeterReadingPermissionUpdateAndFulfillmentService(
                        new EventFulfillmentService(outbox, DkSimpleEvent::new),
                        (reading, end) -> outbox.commit(new DkInternalPollingEvent(reading.permissionId(), end))
                ),
                outbox,
                mapper
        );
    }

    @Test
    void fetchingAConsumptionRecord_revokesPermissionRequest_whenTokenInvalid() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(),
                                                                                    "",
                                                                                    HttpHeaders.EMPTY,
                                                                                    null,
                                                                                    null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @Test
    void fetchingAConsumptionRecord_doesNotRevokePermission_on5xx() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                                    "",
                                                                                    HttpHeaders.EMPTY,
                                                                                    null,
                                                                                    null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox, never()).commit(any());
    }

    @Test
    void fetchingAConsumptionRecord_emitsRecord() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());
        MyEnergyDataMarketDocumentResponse resultItem = new MyEnergyDataMarketDocumentResponse();
        resultItem.setMyEnergyDataMarketDocument(new MyEnergyDataMarketDocument()
                                                         .periodTimeInterval(new PeriodtimeInterval()
                                                                                     .start(start.atStartOfDay(
                                                                                             ZoneOffset.UTC).format(
                                                                                             DateTimeFormatter.ISO_DATE_TIME))
                                                                                     .end(end.atStartOfDay(ZoneOffset.UTC)
                                                                                             .format(DateTimeFormatter.ISO_DATE_TIME)))
        );
        MyEnergyDataMarketDocumentResponseListApiResponse data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(resultItem);
        when(customerApi.getTimeSeries(eq(start),
                                       eq(end.plusDays(1)),
                                       any(),
                                       any(),
                                       eq("token"),
                                       any()))
                .thenReturn(Mono.just(data));

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchHistoricalMeterReadings(permissionRequest))
                    .assertNext(mr -> assertAll(
                            () -> assertEquals(permissionRequest.permissionId(), mr.permissionRequest().permissionId()),
                            () -> assertEquals(permissionRequest.connectionId(), mr.permissionRequest().connectionId()),
                            () -> assertEquals(permissionRequest.dataNeedId(), mr.permissionRequest().dataNeedId()),
                            () -> assertNotNull(mr.apiResponse())
                    ))
                    .then(pollingService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        verify(outbox).commit(pollingEventCaptor.capture());
        var res = pollingEventCaptor.getValue();
        assertEquals(end, res.latestMeterReadingEndDate());
    }

    @Test
    void fetchingInactiveConsumptionRecord_doesNotEmit() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).plusDays(1);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var accessToken = "accessToken";
        var permissionRequest = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                accessToken,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );

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
        var start1 = LocalDate.now(DK_ZONE_ID).minusDays(1);
        var end1 = start1.plusDays(5);
        var start2 = LocalDate.now(DK_ZONE_ID).plusDays(1);
        var end2 = start2.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        var permissionRequest1 = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start1,
                end1,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );

        MyEnergyDataMarketDocumentResponse resultItem = new MyEnergyDataMarketDocumentResponse();
        resultItem.setMyEnergyDataMarketDocument(new MyEnergyDataMarketDocument()
                                                         .periodTimeInterval(new PeriodtimeInterval()
                                                                                     .start(start1.atStartOfDay(
                                                                                             ZoneOffset.UTC).format(
                                                                                             DateTimeFormatter.ISO_DATE_TIME))
                                                                                     .end(end1.atStartOfDay(ZoneOffset.UTC)
                                                                                              .format(DateTimeFormatter.ISO_DATE_TIME)))
        );
        MyEnergyDataMarketDocumentResponseListApiResponse data = new MyEnergyDataMarketDocumentResponseListApiResponse()
                .addResultItem(resultItem);
        when(customerApi.getTimeSeries(eq(start1), any(), any(), any(), eq("token"), any()))
                .thenReturn(Mono.just(data));
        DkEnerginetPermissionRequest permissionRequest2 = new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start2,
                end2,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        doReturn(Mono.just("token"))
                .when(customerApi).accessToken(anyString());

        when(repository.findAllByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(permissionRequest1, permissionRequest2));

        // Then
        StepVerifier.create(pollingService.identifiableMeterReadings())
                    .then(() -> pollingService.fetchFutureMeterReadings())
                    .then(pollingService::close)
                    .assertNext(mr -> assertAll(
                            () -> assertEquals(permissionRequest1.permissionId(),
                                               mr.permissionRequest().permissionId()),
                            () -> assertEquals(permissionRequest1.connectionId(),
                                               mr.permissionRequest().connectionId()),
                            () -> assertEquals(permissionRequest1.dataNeedId(), mr.permissionRequest().dataNeedId()),
                            () -> assertNotNull(mr.apiResponse())
                    ))
                    .verifyComplete();
        verify(outbox).commit(pollingEventCaptor.capture());
        var res = pollingEventCaptor.getValue();
        assertEquals(end1, res.latestMeterReadingEndDate());
    }
}
