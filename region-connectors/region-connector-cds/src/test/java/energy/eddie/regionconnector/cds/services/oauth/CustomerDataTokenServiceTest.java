// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.client.CustomerDataClientCredentials;
import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDataTokenServiceTest {
    private final URI tokenEndpoint = URI.create("http://localhost");
    @Mock
    private OAuthService oAuthService;
    @Mock
    private OAuthCredentialsRepository repository;
    @InjectMocks
    private CustomerDataTokenService service;

    @Test
    void testGetOAuthCredentialsAsync_whereTokenIsStillValid_returnsToken() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var tomorrow = now.plusDays(1);
        var creds = new OAuthCredentials("pid", "refreshToken", "accessToken", tomorrow);
        when(repository.getOAuthCredentialByPermissionId("pid"))
                .thenReturn(creds);
        var customerDataClientCredentials = new CustomerDataClientCredentials("client-id",
                                                                              "client-secret",
                                                                              tokenEndpoint);

        // When
        var res = service.getOAuthCredentialsAsync("pid", customerDataClientCredentials);

        // Then
        StepVerifier.create(res)
                    .assertNext(token -> assertEquals(token, creds))
                    .verifyComplete();
    }

    @Test
    void testGetOAuthCredentialsAsync_whereTokenIsInvalidValidAndOldRefreshTokenCanBeUsed_returnsToken() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var tomorrow = now.plusDays(1);
        var yesterday = now.minusDays(1);
        var oldCreds = new OAuthCredentials("pid", "refreshToken", "accessToken", yesterday);
        var newCreds = new OAuthCredentials("pid", "refreshToken", "newAccessToken", tomorrow);
        when(repository.getOAuthCredentialByPermissionId("pid"))
                .thenReturn(oldCreds);
        when(repository.save(newCreds)).thenReturn(newCreds);
        var creds = new CustomerDataClientCredentials("client-id", "client-secret", tokenEndpoint);
        when(oAuthService.retrieveAccessToken(oldCreds, "client-id", "client-secret", tokenEndpoint))
                .thenReturn(new CredentialsWithoutRefreshToken("newAccessToken", tomorrow));

        // When
        var res = service.getOAuthCredentialsAsync("pid", creds);

        // Then
        StepVerifier.create(res)
                    .assertNext(token -> assertEquals(token, newCreds))
                    .verifyComplete();
    }

    @Test
    void testGetOAuthCredentialsAsync_whereTokenIsInvalidValidAndNewRefreshTokenIsProvided_returnsToken() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var tomorrow = now.plusDays(1);
        var yesterday = now.minusDays(1);
        var oldCreds = new OAuthCredentials("pid", "refreshToken", "accessToken", yesterday);
        var newCreds = new OAuthCredentials("pid", "newRefreshToken", "newAccessToken", tomorrow);
        when(repository.getOAuthCredentialByPermissionId("pid"))
                .thenReturn(oldCreds);
        when(repository.save(newCreds)).thenReturn(newCreds);
        var creds = new CustomerDataClientCredentials("client-id", "client-secret", tokenEndpoint);
        when(oAuthService.retrieveAccessToken(oldCreds, "client-id", "client-secret", tokenEndpoint))
                .thenReturn(new CredentialsWithRefreshToken("newAccessToken", "newRefreshToken", tomorrow));

        // When
        var res = service.getOAuthCredentialsAsync("pid", creds);

        // Then
        StepVerifier.create(res)
                    .assertNext(token -> assertEquals(token, newCreds))
                    .verifyComplete();
    }

    @Test
    void testGetOAuthCredentialsAsync_withInvalidToken_throwsNoTokenException() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var yesterday = now.minusDays(1);
        var oldCreds = new OAuthCredentials("pid", "refreshToken", "accessToken", yesterday);
        when(repository.getOAuthCredentialByPermissionId("pid"))
                .thenReturn(oldCreds);
        var creds = new CustomerDataClientCredentials("client-id", "client-secret", tokenEndpoint);
        when(oAuthService.retrieveAccessToken(oldCreds, "client-id", "client-secret", tokenEndpoint))
                .thenReturn(new InvalidTokenResult());

        // When
        var res = service.getOAuthCredentialsAsync("pid", creds);

        // Then
        StepVerifier.create(res)
                    .expectError(NoTokenException.class)
                    .verify();
    }
}