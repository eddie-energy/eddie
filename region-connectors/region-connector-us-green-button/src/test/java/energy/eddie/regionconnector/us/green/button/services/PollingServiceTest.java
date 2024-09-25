package energy.eddie.regionconnector.us.green.button.services;

import com.rometools.rome.feed.synd.SyndFeedImpl;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.exceptions.DataNotReadyException;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.UsPollingNotReadyEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    private static final List<MeterReading> METERS = List.of(new MeterReading("pid", "uid", null));
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private GreenButtonApi api;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private CredentialService credentialService;
    @Mock
    private PublishService publishService;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private PollingService pollingService;

    public static Stream<Arguments> pollWithException_doesNothing() {
        return Stream.of(
                Arguments.of(new Exception()),
                Arguments.of(WebClientResponseException.create(HttpStatus.UNAUTHORIZED, "", null, null, null, null))
        );
    }

    @Test
    void pollOfValidatedHistoricalData_publishes() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                METERS,
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.batchSubscription("1111",
                                   "token",
                                   Set.of("uid"),
                                   start.atStartOfDay(ZoneOffset.UTC),
                                   DateTimeUtils.endOfDay(end, ZoneOffset.UTC)))
                .thenReturn(Flux.just(new SyndFeedImpl()));

        // When
        pollingService.poll("pid");

        // Then
        verify(publishService).publish(any());
    }

    @Test
    void pollOfValidatedHistoricalData_forFuture_publishes() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                METERS,
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.plusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.batchSubscription("1111",
                                   "token",
                                   Set.of("uid"),
                                   start.atStartOfDay(ZoneOffset.UTC),
                                   LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC)))
                .thenReturn(Flux.just(new SyndFeedImpl()));

        // When
        pollingService.poll("pid");

        // Then
        verify(publishService).publish(any());
    }

    @Test
    void pollOfValidatedHistoricalData_whereSomeDataAlreadyHasBeenPolled() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var actualStart = ZonedDateTime.now(ZoneOffset.UTC).minusDays(8);
        var meters = List.of(
                new MeterReading("pid", "1", actualStart),
                new MeterReading("pid", "2", ZonedDateTime.now(ZoneOffset.UTC).minusDays(1))
        );
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                meters,
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.plusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.batchSubscription("1111",
                                   "token",
                                   Set.of("2", "1"),
                                   actualStart,
                                   LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC)))
                .thenReturn(Flux.just(new SyndFeedImpl(), new SyndFeedImpl()));

        // When
        pollingService.poll("pid");

        // Then
        verify(publishService, times(2)).publish(any());
    }

    @Test
    void pollOfInactivePermission_doesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now.plusDays(1),
                now.plusDays(2),
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        pollingService.poll("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publish(any());
    }

    @Test
    void pollOfUnsupportedDataNeed_doesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var calc = new DataNeedNotSupportedResult("");
        when(calculationService.calculate("dnid")).thenReturn(calc);

        // When
        pollingService.poll("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publish(any());
    }


    @Test
    void pollWithCredentialsWithoutRefreshToken_emitsUnfulfillableEvent() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.error(new NoRefreshTokenException()));

        // When
        pollingService.poll("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publish(any());
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }

    @Test
    void pollWithUnknownExceptionDoesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.error(new Exception()));

        // When
        pollingService.poll("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publish(any());
        verify(outbox, never()).commit(any());
    }

    @Test
    void poll_withForbidden_revokesPermissionRequest() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.error(
                WebClientResponseException.create(HttpStatus.FORBIDDEN, "", null, null, null, null)));

        // When
        pollingService.poll("pid");

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @Test
    void pollWithNonValidatedHistoricalDataNeed_doesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var calc = new AccountingPointDataNeedResult(new Timeframe(now, now));
        when(calculationService.calculate("dnid")).thenReturn(calc);

        // When
        pollingService.poll("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publish(any());
    }

    @Test
    void pollWithForbidden_revokesPermissionRequest() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.batchSubscription(any(), any(), any(), any(), any()))
                .thenReturn(Flux.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)));

        // When
        pollingService.poll("pid");

        // Then
        verify(publishService, never()).publish(any());
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @Test
    void pollWithDataNotReady_emitsPollingNotReadyEvent() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.batchSubscription(any(), any(), any(), any(), any()))
                .thenReturn(Flux.error(new DataNotReadyException()));

        // When
        pollingService.poll("pid");

        // Then
        verify(publishService, never()).publish(any());
        verify(outbox).commit(isA(UsPollingNotReadyEvent.class));
    }

    @ParameterizedTest
    @MethodSource
    void pollWithException_doesNothing(Exception e) {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(start, end)
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.batchSubscription(any(), any(), any(), any(), any()))
                .thenReturn(Flux.error(e));

        // When
        pollingService.poll("pid");

        // Then
        verify(publishService, never()).publish(any());
        verify(outbox, never()).commit(any());
    }
}