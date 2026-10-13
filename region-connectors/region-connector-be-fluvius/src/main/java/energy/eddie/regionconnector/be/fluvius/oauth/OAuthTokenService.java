// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import jakarta.annotation.Nullable;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OAuthTokenService {
    private final FluviusOAuthConfiguration config;
    private final ClientAuthenticationFactory authenticationFactory;
    @Nullable
    private String accessToken = null;
    private Instant expiresAt = Instant.MIN;

    public OAuthTokenService(FluviusOAuthConfiguration config, SslBundles sslBundles) {
        this.config = config;
        switch (config.authenticationMethod()) {
            case CLIENT_SECRET -> {
                var authentication = new ClientSecretBasic(new ClientID(config.clientId()),
                                                           new Secret(Objects.requireNonNull(config.clientSecret())));
                this.authenticationFactory = () -> authentication;
            }
            case CERTIFICATE -> {
                var authentication = new CertificateClientAuthentication(config.clientId(),
                                                                         URI.create(config.tokenUrl()),
                                                                         sslBundles.getBundle(config.sslBundle()));
                this.authenticationFactory = authentication::create;
            }
            default -> throw new IllegalArgumentException("Unsupported Fluvius OAuth authentication method");
        }
    }

    public synchronized String accessToken() throws URISyntaxException, IOException, ParseException, OAuthException {
        var now = Instant.now();
        if (accessToken != null && expiresAt.isAfter(now.plusSeconds(60))) {
            return accessToken;
        }
        var clientGrant = new ClientCredentialsGrant();
        var scope = new Scope(config.scope());
        var tokenEndpoint = new URI(config.tokenUrl());
        final ClientAuthentication authentication;
        try {
            authentication = authenticationFactory.create();
        } catch (GeneralSecurityException | JOSEException exception) {
            throw new OAuthException("Cannot sign Fluvius OAuth client assertion", exception);
        }
        var request = new TokenRequest(tokenEndpoint, authentication, clientGrant, scope, null,
                                       Map.of("client_id", List.of(config.clientId())));
        var response = TokenResponse.parse(request.toHTTPRequest().send());
        if (!response.indicatesSuccess()) {
            var errorResponse = response.toErrorResponse();
            throw new OAuthException(errorResponse.toString());
        }
        var successResponse = response.toSuccessResponse();
        accessToken = successResponse.getTokens().getAccessToken().getValue();
        expiresAt = now.plusSeconds(successResponse.getTokens().getAccessToken().getLifetime());
        return accessToken;
    }

    @FunctionalInterface
    private interface ClientAuthenticationFactory {
        ClientAuthentication create() throws GeneralSecurityException, JOSEException;
    }
}
