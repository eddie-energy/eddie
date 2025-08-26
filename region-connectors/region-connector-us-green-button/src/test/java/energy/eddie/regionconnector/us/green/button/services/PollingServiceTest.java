package energy.eddie.regionconnector.us.green.button.services;

import com.rometools.rome.feed.synd.SyndFeedImpl;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
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
import reactor.test.StepVerifier;

import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    private static final List<MeterReading> METERS = List.of(new MeterReading("pid", "uid", null,
                                                                              PollingStatus.DATA_NOT_READY));
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private GreenButtonApi api;
    @Mock
    private CredentialService credentialService;
    @Mock
    private PublishService publishService;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private PollingService pollingService;

    public static Stream<Arguments> pollValidatedHistoricalDataWithException_doesNothing() {
        return Stream.of(
                Arguments.of(new Exception()),
                Arguments.of(WebClientResponseException.create(HttpStatus.UNAUTHORIZED, "", null, null, null, null))
        );
    }

    @Test
    void pollValidatedHistoricalDataOfValidatedHistoricalData_publishes() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(start)
                                                          .setEnd(end)
                                                          .setLastMeterReadings(METERS)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
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
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(publishService).publishValidatedHistoricalData(any());
    }

    @Test
    void pollValidatedHistoricalDataOfValidatedHistoricalData_forFuture_publishes() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var end = now.plusDays(1);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(start)
                                                          .setEnd(end)
                                                          .setLastMeterReadings(METERS)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
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
                                   now.atStartOfDay(ZoneOffset.UTC)))
                .thenReturn(Flux.just(new SyndFeedImpl()));

        // When
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(publishService).publishValidatedHistoricalData(any());
    }

    @Test
    void pollValidatedHistoricalDataOfValidatedHistoricalData_whereSomeDataAlreadyHasBeenPolled() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var end = now.plusDays(1);
        var actualStart = ZonedDateTime.now(ZoneOffset.UTC).minusDays(8);
        var meters = List.of(
                new MeterReading("pid", "1", actualStart, PollingStatus.DATA_NOT_READY),
                new MeterReading("pid", "2", ZonedDateTime.now(ZoneOffset.UTC).minusDays(1),
                                 PollingStatus.DATA_NOT_READY)
        );
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(start)
                                                          .setEnd(end)
                                                          .setLastMeterReadings(meters)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
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
                                   now.atStartOfDay(ZoneOffset.UTC)))
                .thenReturn(Flux.just(new SyndFeedImpl(), new SyndFeedImpl()));

        // When
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(publishService, times(2)).publishValidatedHistoricalData(any());
    }

    @Test
    void pollValidatedHistoricalDataOfInactivePermission_doesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(now.plusDays(1))
                                                          .setEnd(now.plusDays(2))
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publishValidatedHistoricalData(any());
    }

    @Test
    void pollValidatedHistoricalDataWithCredentialsWithoutRefreshToken_emitsUnfulfillableEvent() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(now)
                                                          .setEnd(now)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.error(new NoRefreshTokenException()));

        // When
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publishValidatedHistoricalData(any());
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }

    @Test
    void pollValidatedHistoricalDataWithUnknownExceptionDoesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(now)
                                                          .setEnd(now)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.error(new Exception()));

        // When
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(api, never()).batchSubscription(any(), any(), any(), any(), any());
        verify(publishService, never()).publishValidatedHistoricalData(any());
        verify(outbox, never()).commit(any());
    }

    @Test
    void pollValidatedHistoricalData_withForbidden_revokesPermissionRequest() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(now)
                                                          .setEnd(now)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.error(
                WebClientResponseException.create(HttpStatus.FORBIDDEN, "", null, null, null, null)));

        // When
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @Test
    void pollValidatedHistoricalDataWithForbidden_revokesPermissionRequest() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(now)
                                                          .setEnd(now)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
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
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(publishService, never()).publishValidatedHistoricalData(any());
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @ParameterizedTest
    @MethodSource
    void pollValidatedHistoricalDataWithException_doesNothing(Exception e) {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setStart(now)
                                                          .setEnd(now)
                                                          .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
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
        pollingService.pollValidatedHistoricalData("pid");

        // Then
        verify(publishService, never()).publishValidatedHistoricalData(any());
        verify(outbox, never()).commit(any());
    }


    @Test
    void forcePollValidatedHistoricalDataOfValidatedHistoricalData_publishes() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                          .setLastMeterReadings(METERS)
                                                          .build();
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.batchSubscription("1111", "token", Set.of("uid"), start, end))
                .thenReturn(Flux.just(new SyndFeedImpl()));

        // When
        var res = pollingService.forcePollValidatedHistoricalData(pr, start, end);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
        verify(publishService).publishValidatedHistoricalData(any());
    }

    @Test
    void pollAccountingPointData_emitsAccountingPointData() {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder().
                setPermissionId("pid")
                .setAuthUid("1111")
                .build();
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        var feed = new SyndFeedImpl();
        when(api.retailCustomer("1111", "token"))
                .thenReturn(Mono.just(feed));

        // When
        pollingService.pollAccountingPointData(pr);

        // Then
        verify(publishService).publishAccountingPointData(new IdentifiableSyndFeed(pr, feed));
    }

    @Test
    void pollAccountingPointData_withForbidden_revokesPermissionRequest() {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.error(
                WebClientResponseException.create(HttpStatus.FORBIDDEN, "", null, null, null, null)));

        // When
        pollingService.pollAccountingPointData(pr);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    @Test
    void pollAccountingPointDataWithForbidden_revokesPermissionRequest() {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder()
                .setAuthUid("1111")
                .setPermissionId("pid")
                .build();
        var credentials = new OAuthTokenDetails("pid",
                                                "token",
                                                Instant.now(Clock.systemUTC()),
                                                Instant.now(Clock.systemUTC()),
                                                "token",
                                                "1111");
        when(credentialService.retrieveAccessToken(pr)).thenReturn(Mono.just(credentials));
        when(api.retailCustomer("1111", "token"))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)));

        // When
        pollingService.pollAccountingPointData(pr);

        // Then
        verify(publishService, never()).publishValidatedHistoricalData(any());
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }
}