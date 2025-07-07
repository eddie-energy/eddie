package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.fi.fingrid.TestResourceProvider.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    @Mock
    private EnergyDataService energyDataService;
    @Mock
    private FingridApiClient api;
    @Mock
    private UpdateGranularityService updateGranularityService;
    @Mock
    private Outbox outbox;
    @Mock
    @SuppressWarnings("unused")
    private DataNeedsService dataNeedsService; //this is needed to mock the PollingService
    @InjectMocks
    private PollingService pollingService;

    private static Stream<Arguments> pollValidatedHistoricalData_doesNothing_onFuturePermissionRequests() {
        return Stream.of(
                Arguments.of(LocalDate.now(ZoneOffset.UTC)),
                Arguments.of(LocalDate.now(ZoneOffset.UTC).plusDays(1))
        );
    }

    @ParameterizedTest
    @MethodSource("pollValidatedHistoricalData_doesNothing_onFuturePermissionRequests")
    void pollValidatedHistoricalData_doesNothing_onFuturePermissionRequests(LocalDate start) {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(start)
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(null)
                                                      .createFingridPermissionRequest();

        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService, never()).publish(pr);
    }

    @Test
    void pollValidatedHistoricalData_publishesData() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(start)
                                                      .setEnd(now)
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(Map.of())
                                                      .createFingridPermissionRequest();
        var customerData = readCustomerDataFromFile(CUSTOMER_DATA_JSON);
        when(api.getCustomerData("cid")).thenReturn(Mono.just(customerData));
        var data = new TimeSeriesResponse(null);
        var resp = Mono.just(data);
        when(api.getTimeSeriesData(any(), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(Mono.just(List.of(data)));
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService).publish(pr);
    }

    @Test
    void pollValidatedHistoricalData_usesStartDate_ifNoLatestMeterReadingIsAvailable() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(start)
                                                      .setEnd(now)
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(Map.of())
                                                      .createFingridPermissionRequest();
        var customerData = readCustomerDataFromFile(CUSTOMER_DATA_JSON);
        when(api.getCustomerData("cid")).thenReturn(Mono.just(customerData));
        var data = new TimeSeriesResponse(null);
        var resp = Mono.just(data);
        when(api.getTimeSeriesData(any(), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(Mono.just(List.of(data)));
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(api)
                .getTimeSeriesData("642502030590623827",
                                   "cid",
                                   start.atStartOfDay(ZoneOffset.UTC),
                                   DateTimeUtils.endOfDay(now.minusDays(1), ZoneOffset.UTC),
                                   "PT1H",
                                   null);
    }

    @Test
    void pollValidatedHistoricalData_usesYesterday_ifEndIsInTheFuture() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(start)
                                                      .setEnd(now.plusDays(10))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(Map.of())
                                                      .createFingridPermissionRequest();
        var data = new TimeSeriesResponse(null);
        var resp = Mono.just(data);
        when(api.getTimeSeriesData(any(), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        var customerData = readCustomerDataFromFile(CUSTOMER_DATA_JSON);
        when(api.getCustomerData("cid")).thenReturn(Mono.just(customerData));
        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(Mono.just(List.of(data)));
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(api)
                .getTimeSeriesData("642502030590623827",
                                   "cid",
                                   start.atStartOfDay(ZoneOffset.UTC),
                                   DateTimeUtils.endOfDay(now.minusDays(1), ZoneOffset.UTC),
                                   "PT1H",
                                   null);
    }

    @Test
    void pollValidatedHistoricalData_usesLastMeteringData_ifAvailable() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var latestMeteringData = now.minusDays(5).atStartOfDay(ZoneOffset.UTC);
        var end = now.minusDays(1);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(start)
                                                      .setEnd(end)
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(Map.of("mid", latestMeteringData))
                                                      .createFingridPermissionRequest();
        var data = new TimeSeriesResponse(null);
        var resp = Mono.just(data);
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(Mono.just(List.of(data)));
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(api)
                .getTimeSeriesData(
                        "mid",
                        "cid",
                        latestMeteringData,
                        DateTimeUtils.endOfDay(end, ZoneOffset.UTC),
                        "PT1H",
                        null
                );
    }

    @Test
    @SuppressWarnings("DirectInvocationOnMock")
        // Mock is not directly invoked, but reference to method is provided
    void pollValidatedHistoricalData_withEmptyResponse_doesNotPublish() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var latestMeteringData = now.minusDays(5).atStartOfDay(ZoneOffset.UTC);
        var end = now.minusDays(1);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(start)
                                                      .setEnd(end)
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(Map.of("mid", latestMeteringData))
                                                      .createFingridPermissionRequest();
        var resp = Mono.just(new TimeSeriesResponse(null));
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(resp);

        when(updateGranularityService.updateGranularity(any(), eq(pr)))
                .thenReturn(Mono.empty());
        when(energyDataService.publish(pr)).thenReturn(e -> energyDataService.publish(e, pr));
        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService, never())
                .publish(anyList(), any());
    }

    @Test
    @SuppressWarnings("DirectInvocationOnMock")
        // Mock is not directly invoked, but reference to method is provided
    void pollValidatedHistoricalData_withErrorResponse_doesNotPublish() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var latestMeteringData = now.minusDays(5).atStartOfDay(ZoneOffset.UTC);
        var end = now.minusDays(1);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(start)
                                                      .setEnd(end)
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(Map.of("mid", latestMeteringData))
                                                      .createFingridPermissionRequest();
        when(api.getTimeSeriesData(eq("mid"), eq("cid"), any(), any(), eq("PT1H"), eq(null)))
                .thenReturn(Mono.error(new RuntimeException()));

        when(energyDataService.publish(pr)).thenReturn(e -> energyDataService.publish(e, pr));

        // When
        pollingService.pollTimeSeriesData(pr);

        // Then
        verify(energyDataService, never())
                .publish(anyList(), any());
    }

    @Test
    void pollAccountingPointData_pollsAccountingPointData() {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCustomerIdentification("cid")
                                                      .createFingridPermissionRequest();
        var data = readCustomerDataFromFile(CUSTOMER_DATA_JSON);
        when(api.getCustomerData("cid")).thenReturn(Mono.just(data));
        // When
        pollingService.pollAccountingPointData(pr);

        // Then
        verify(energyDataService).publish(data, pr);
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"FORBIDDEN", "UNAUTHORIZED"})
    void pollAccountingPointData_forUnauthorizedUser_revokesPermission(HttpStatus status) {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCustomerIdentification("cid")
                                                      .createFingridPermissionRequest();
        var exception = WebClientResponseException.create(
                status.value(),
                "",
                null,
                null,
                null
        );
        when(api.getCustomerData("cid")).thenReturn(Mono.error(exception));

        // When
        pollingService.pollAccountingPointData(pr);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @Test
    void pollAccountingPointData_forEmptyResponse_emitsUnfulfillable() {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCustomerIdentification("cid")
                                                      .createFingridPermissionRequest();
        var data = TestResourceProvider.readCustomerDataFromFile(EMPTY_CUSTOMER_DATA_JSON);
        when(api.getCustomerData("cid")).thenReturn(Mono.just(data));

        // When
        pollingService.pollAccountingPointData(pr);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }
}