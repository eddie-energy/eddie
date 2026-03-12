package energy.eddie.regionconnector.de.eta.auth;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;

@Service
public class EtaAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtaAuthService.class);

    private final DeEtaPlusConfiguration configuration;

    public EtaAuthService(DeEtaPlusConfiguration configuration) {
        this.configuration = configuration;
    }

    public Mono<AuthTokenResponse> exchangeCodeForToken(String code, String openid) {
        return Mono.fromCallable(() -> performTokenExchange(code, openid))
                   .subscribeOn(Schedulers.boundedElastic())
                   .onErrorResume(error -> {
                       LOGGER.error("Error during token exchange", error);
                       return Mono.just(new AuthTokenResponse(null, false));
                   });
    }

    private AuthTokenResponse performTokenExchange(String code, String openid)
            throws IOException, ParseException {

        LOGGER.info("Exchanging authorization token for access token");

        AuthorizationCodeGrant codeGrant = new AuthorizationCodeGrant(
                new AuthorizationCode(code),
                URI.create(openid));

        URI tokenEndpoint = URI.create(configuration.auth().tokenUrl());
        ClientID clientID = new ClientID(configuration.auth().clientId());

        TokenRequest request = new TokenRequest(tokenEndpoint, clientID, codeGrant);
        HTTPRequest httpRequest = request.toHTTPRequest();
        httpRequest.setAccept("application/json");

        HTTPResponse response = httpRequest.send();

        TokenResponse tokenResponse = TokenResponse.parse(response);

        if (!tokenResponse.indicatesSuccess()) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            ErrorObject errorObject = errorResponse.getErrorObject();

            if (errorObject != null && errorObject.getCode() != null) {
                LOGGER.warn("Token exchange unsuccessful: {}", errorObject.getCode());
            } else {
                LOGGER.warn("Token exchange unsuccessful with unknown error");
            }

            return new AuthTokenResponse(null, false);
        }

        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();

        String token = successResponse.getTokens()
                                      .getAccessToken()
                                      .getValue();

        long expiresIn = successResponse.getTokens()
                                        .getAccessToken()
                                        .getLifetime();

        String refreshTokenString = null;
        if (successResponse.getTokens().getRefreshToken() != null) {
            refreshTokenString = successResponse.getTokens()
                                                .getRefreshToken()
                                                .getValue();
        }

        LOGGER.info("Successfully exchanged token for access token. Expires in: {} seconds", expiresIn);

        return new AuthTokenResponse(
                new AuthTokenResponse.TokenData(token, refreshTokenString),
                true
        );
    }
}