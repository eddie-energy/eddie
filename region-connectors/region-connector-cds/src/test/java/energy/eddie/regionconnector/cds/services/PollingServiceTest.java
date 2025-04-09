package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClient;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientErrorHandler;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientFactory;
import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    @Spy
    private final IdentifiableDataStreams streams = new IdentifiableDataStreams();
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private CustomerDataClientFactory factory;
    @Mock
    private CustomerDataClient customerDataClient;
    @Mock
    private CustomerDataClientErrorHandler handler;
    @InjectMocks
    private PollingService pollingService;

    static Stream<Arguments> testPoll_forActivePermissionRequest_emitsUsageSegments() {
        var today = LocalDate.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(today.minusDays(1)),
                Arguments.of(today.plusDays(1))
        );
    }

    static Stream<Arguments> exceptionSupplier() {
        return Stream.of(
                Arguments.of(WebClientResponseException.create(HttpStatus.UNAUTHORIZED,
                                                               "status",
                                                               null,
                                                               null,
                                                               null,
                                                               null)),
                Arguments.of(new NoTokenException())
        );
    }

    @Test
    void testPoll_forUnsupportedDataNeed_doesNothing() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var start = today.minusWeeks(1);
        var end = today.minusDays(1);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataStart(start)
                .setDataEnd(end)
                .setDataNeedId("dnid")
                .setCreated(now)
                .build();
        when(calculationService.calculate("dnid", now))
                .thenReturn(new DataNeedNotSupportedResult("not supported"));

        // When
        pollingService.poll(pr);

        // Then
        verify(handler, never()).revoke(any(), any());
        verify(streams, never()).publishAccountingPointData(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPoll_forInactivePermissionRequest_doesNothing() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var start = today.plusDays(1);
        var end = today.plusWeeks(1);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataStart(start)
                .setDataEnd(end)
                .setDataNeedId("dnid")
                .setCreated(now)
                .build();
        when(calculationService.calculate("dnid", now))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                      new Timeframe(start, end),
                                                                      new Timeframe(start, end)));

        // When
        pollingService.poll(pr);

        // Then
        verify(handler, never()).revoke(any(), any());
        verify(streams, never()).publishAccountingPointData(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource
    void testPoll_forActivePermissionRequest_emitsUsageSegments(LocalDate end) {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var start = today.minusWeeks(1);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataStart(start)
                .setDataEnd(end)
                .setDataNeedId("dnid")
                .setCreated(now)
                .setCdsServer(1)
                .build();
        var calcResult = new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                   new Timeframe(start, today),
                                                                   new Timeframe(start, end));
        when(calculationService.calculate("dnid", now))
                .thenReturn(calcResult);
        when(factory.get(pr))
                .thenReturn(customerDataClient);
        when(customerDataClient.usagePoints(eq(pr), any(), eq(start.atStartOfDay(ZoneOffset.UTC))))
                .thenReturn(Mono.just(List.of()));
        when(customerDataClient.accounts(pr)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.serviceContracts(pr)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.servicePoints(pr)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.meterDevices(pr)).thenReturn(Mono.just(List.of()));
        when(handler.thenRevoke(any())).thenCallRealMethod();
        // When
        pollingService.poll(pr);

        // Then
        verify(handler, never()).revoke(any(), any());
        verify(streams).publishValidatedHistoricalData(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("exceptionSupplier")
    void testPoll_forRevokedPermissionRequest_doesNothing(Exception exception) {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var start = today.minusWeeks(1);
        var end = today.minusDays(1);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataStart(start)
                .setDataEnd(end)
                .setDataNeedId("dnid")
                .setCreated(now)
                .setCdsServer(1)
                .build();
        var calcResult = new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                   new Timeframe(start, today),
                                                                   new Timeframe(start, end));
        when(calculationService.calculate("dnid", now))
                .thenReturn(calcResult);
        when(factory.get(pr))
                .thenReturn(customerDataClient);
        when(customerDataClient.accounts(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.serviceContracts(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.servicePoints(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.meterDevices(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.usagePoints(pr, endOfDay(end, ZoneOffset.UTC), start.atStartOfDay(ZoneOffset.UTC)))
                .thenReturn(Mono.error(exception));
        when(handler.thenRevoke(any())).thenCallRealMethod();
        when(handler.test(any())).thenCallRealMethod();

        // When
        pollingService.poll(pr);

        // Then
        verify(handler).revoke(any(), any());
        verify(streams, never()).publishAccountingPointData(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPoll_whereClientReturnsUnknownException_doesNothing() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var start = today.minusWeeks(1);
        var end = today.minusDays(1);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataStart(start)
                .setDataEnd(end)
                .setDataNeedId("dnid")
                .setCreated(now)
                .setCdsServer(1)
                .build();
        var calcResult = new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                   new Timeframe(start, today),
                                                                   new Timeframe(start, end));
        when(calculationService.calculate("dnid", now))
                .thenReturn(calcResult);
        when(factory.get(pr)).thenReturn(customerDataClient);
        when(customerDataClient.usagePoints(pr, endOfDay(end, ZoneOffset.UTC), start.atStartOfDay(ZoneOffset.UTC)))
                .thenReturn(Mono.error(new RuntimeException()));
        when(handler.test(any())).thenCallRealMethod();
        when(handler.thenRevoke(any())).thenCallRealMethod();

        // When
        pollingService.poll(pr);

        // Then
        verify(handler, never()).revoke(any(), any());
        verify(streams, never()).publishAccountingPointData(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("exceptionSupplier")
    void testPoll_forRevokedPermissionRequest_forAccountingPointData_doesNothing(Exception exception) {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var start = today.minusWeeks(1);
        var end = today.minusDays(1);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataStart(start)
                .setDataEnd(end)
                .setDataNeedId("dnid")
                .setCreated(now)
                .setCdsServer(1)
                .build();
        var calcResult = new AccountingPointDataNeedResult(new Timeframe(start, end));
        when(calculationService.calculate("dnid", now)).thenReturn(calcResult);
        when(factory.get(pr)).thenReturn(customerDataClient);
        when(customerDataClient.accounts(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.serviceContracts(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.servicePoints(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.meterDevices(pr)).thenReturn(Mono.error(exception));
        when(customerDataClient.billSections(pr)).thenReturn(Mono.error(exception));
        when(handler.thenRevoke(any())).thenCallRealMethod();
        when(handler.test(any())).thenCallRealMethod();

        // When
        pollingService.poll(pr);

        // Then
        verify(handler).revoke(any(), any());
        verify(streams, never()).publishAccountingPointData(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPoll_forAccountingPointPermissionRequest_emitsData() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCreated(now)
                .setCdsServer(1)
                .build();
        var calcResult = new AccountingPointDataNeedResult(new Timeframe(today, today));
        when(calculationService.calculate("dnid", now)).thenReturn(calcResult);
        when(factory.get(pr)).thenReturn(customerDataClient);
        when(customerDataClient.accounts(pr)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.serviceContracts(pr)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.servicePoints(pr)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.meterDevices(pr)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.billSections(pr)).thenReturn(Mono.just(List.of()));
        when(handler.thenRevoke(any())).thenCallRealMethod();
        // When
        pollingService.poll(pr);

        // Then
        verify(handler, never()).revoke(any(), any());
        verify(streams).publishAccountingPointData(any(), any(), any(), any(), any(), any());
    }
}