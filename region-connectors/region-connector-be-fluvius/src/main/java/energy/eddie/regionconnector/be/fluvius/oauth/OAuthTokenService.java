// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.shared.jwt.JwtValidations.isValidUntil;

@Service
public class OAuthTokenService {
    private final FluviusOAuthConfiguration config;
    @Nullable
    private String accessToken = null;

    @Autowired
    public OAuthTokenService(FluviusOAuthConfiguration config) {
        this(config, null);
    }

    OAuthTokenService(FluviusOAuthConfiguration config, @Nullable String accessToken) {
        this.config = config;
        this.accessToken = accessToken;
    }

    public String accessToken() throws URISyntaxException, IOException, ParseException, OAuthException {
        var nowPlusOneMinute = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(1);
        if (accessToken != null && isValidUntil(accessToken, nowPlusOneMinute)) {
            return accessToken;
        }
        var clientGrant = new ClientCredentialsGrant();
        var clientID = new ClientID(config.clientId());
        var clientSecret = new Secret(config.clientSecret());
        var clientAuth = new ClientSecretBasic(clientID, clientSecret);
        var scope = new Scope(config.scope());
        var tokenEndpoint = new URI(config.tokenUrl());
        var request = new TokenRequest(tokenEndpoint, clientAuth, clientGrant, scope);
        var response = TokenResponse.parse(request.toHTTPRequest().send());
        if (!response.indicatesSuccess()) {
            var errorResponse = response.toErrorResponse();
            throw new OAuthException(errorResponse.toString());
        }
        var successResponse = response.toSuccessResponse();
        accessToken = successResponse.getTokens().getAccessToken().getValue();
        return accessToken;
    }
}
