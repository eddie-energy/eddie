package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import energy.eddie.regionconnector.us.green.button.api.TokenApi;
import energy.eddie.regionconnector.us.green.button.client.OAuthTokenClientFactory;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.exceptions.InvalidScopesException;
import energy.eddie.regionconnector.us.green.button.oauth.dto.AccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithCodeRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithRefreshTokenRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {
    @Mock
    private OAuthTokenRepository repository;
    @SuppressWarnings("unused")
    @Spy
    private GreenButtonConfiguration config = new GreenButtonConfiguration("token",
                                                                           "http://localhost",
                                                                           Map.of("company", "client-id"),
                                                                           Map.of("compand", "client-secret"),
                                                                           "http://localhost");
    @SuppressWarnings("unused")
    @Mock
    private Outbox outbox;
    @Mock
    private OAuthTokenClientFactory factory;
    @Mock
    private TokenApi tokenApi;
    @InjectMocks
    private CredentialService credentialService;

    @Test
    void retrieveExistingValidAccessToken_returnsToken() {
        // Given
        var now = Instant.now(Clock.systemUTC());
        var credentials = new OAuthTokenDetails(
                "pid",
                "tokenValue",
                now,
                now.plus(10, ChronoUnit.DAYS),
                null,
                "1111"
        );
        when(repository.getReferenceById("pid"))
                .thenReturn(credentials);
        var pr = createPermissionRequest("company");

        // When
        var res = credentialService.retrieveAccessToken(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(token -> assertEquals(credentials.accessToken(), token.accessToken()))
                    .verifyComplete();
    }

    private static @NotNull GreenButtonPermissionRequest createPermissionRequest(String companyId) {
        var today = LocalDate.now(ZoneOffset.UTC);
        return new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                today,
                today,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                today.atStartOfDay(ZoneOffset.UTC),
                "US",
                companyId,
                "http://localhost",
                "other"
        );
    }

    @Test
    void retrieveExistingInvalidAccessToken_fetchesNewToken() throws MissingClientIdException, MissingClientSecretException {
        // Given
        var now = Instant.now(Clock.systemUTC());
        var credentials = new OAuthTokenDetails(
                "pid",
                "tokenValue",
                now,
                now.minus(10, ChronoUnit.DAYS),
                "refreshToken",
                "1111"
        );
        when(repository.getReferenceById("pid"))
                .thenReturn(credentials);
        when(factory.create("company", "http://localhost"))
                .thenReturn(tokenApi);
        when(tokenApi.accessToken(any(AccessTokenWithRefreshTokenRequest.class)))
                .thenReturn(Mono.just(new AccessTokenResponse("accessToken",
                                                              "newTokenValue",
                                                              null,
                                                              10000,
                                                              "other",
                                                              "",
                                                              "",
                                                              "")));
        var pr = createPermissionRequest("company");

        // When
        var res = credentialService.retrieveAccessToken(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(token -> assertEquals("newTokenValue", token.accessToken()))
                    .verifyComplete();
    }

    @Test
    void retrieveExistingInvalidAccessTokenWithoutRefreshToken_emitsError() {
        // Given
        var now = Instant.now(Clock.systemUTC());
        var credentials = new OAuthTokenDetails(
                "pid",
                "tokenValue",
                now,
                now.minus(10, ChronoUnit.DAYS),
                null,
                "1111"
        );
        when(repository.getReferenceById("pid"))
                .thenReturn(credentials);
        var pr = createPermissionRequest("company");

        // When
        var res = credentialService.retrieveAccessToken(pr);

        // Then
        StepVerifier.create(res)
                    .expectError(NoRefreshTokenException.class)
                    .verify();
    }

    @Test
    void retrieveAccessToken_withMissingCompanyCredentials_emitsError() throws MissingClientIdException, MissingClientSecretException {
        // Given
        var now = Instant.now(Clock.systemUTC());
        var credentials = new OAuthTokenDetails(
                "pid",
                "tokenValue",
                now,
                now.minus(10, ChronoUnit.DAYS),
                "refreshToken",
                "1111"
        );
        when(repository.getReferenceById("pid"))
                .thenReturn(credentials);
        when(factory.create("unknown-company", "http://localhost"))
                .thenThrow(MissingClientIdException.class);
        var pr = createPermissionRequest("unknown-company");

        // When
        var res = credentialService.retrieveAccessToken(pr);

        // Then
        StepVerifier.create(res)
                    .expectError(MissingClientIdException.class)
                    .verify();
    }

    @Test
    void retrieveAccessToken_withCodeAndMatchingScope_emitsCredentials() throws MissingClientIdException, MissingClientSecretException {
        // Given
        when(factory.create("company", "http://localhost"))
                .thenReturn(tokenApi);
        when(tokenApi.accessToken(any(AccessTokenWithCodeRequest.class)))
                .thenReturn(Mono.just(new AccessTokenResponse("accessToken",
                                                              "newTokenValue",
                                                              null,
                                                              10000,
                                                              "other",
                                                              "",
                                                              "",
                                                              "")));
        var pr = createPermissionRequest("company");

        // When
        var res = credentialService.retrieveAccessToken(pr, "code");

        // Then
        StepVerifier.create(res)
                    .assertNext(token -> assertEquals("newTokenValue", token.accessToken()))
                    .verifyComplete();
        verify(repository).save(any(OAuthTokenDetails.class));
    }

    @Test
    void retrieveAccessToken_withInvalidScope_emitsError() throws MissingClientIdException, MissingClientSecretException {
        // Given
        when(factory.create("company", "http://localhost"))
                .thenReturn(tokenApi);
        when(tokenApi.accessToken(any(AccessTokenWithCodeRequest.class)))
                .thenReturn(Mono.just(new AccessTokenResponse("accessToken",
                                                              "newTokenValue",
                                                              null,
                                                              10000,
                                                              "changed-scope",
                                                              "",
                                                              "",
                                                              "")));
        var pr = createPermissionRequest("company");

        // When
        var res = credentialService.retrieveAccessToken(pr, "code");

        // Then
        StepVerifier.create(res)
                    .expectError(InvalidScopesException.class)
                    .verify();
    }
}