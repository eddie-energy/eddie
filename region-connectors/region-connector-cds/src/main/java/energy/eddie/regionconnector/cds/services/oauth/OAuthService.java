package energy.eddie.regionconnector.cds.services.oauth;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.services.oauth.code.AuthorizationCodeResult;
import energy.eddie.regionconnector.cds.services.oauth.par.ErrorParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.ParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.cds.services.oauth.token.Credentials;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import energy.eddie.regionconnector.cds.services.oauth.token.TokenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class OAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthService.class);
    private final CdsConfiguration config;

    public OAuthService(CdsConfiguration config) {this.config = config;}

    public ParResponse pushAuthorization(CdsServer cdsServer, List<String> scopes) {
        var clientID = new ClientID(cdsServer.clientId());
        var clientSecret = new Secret(cdsServer.clientSecret());
        var state = new State();
        var parUri = cdsServer.pushedAuthorizationRequestEndpoint();
        var authzReq = new AuthorizationRequest.Builder(ResponseType.CODE, clientID)
                .endpointURI(parUri)
                .scope(Scope.parse(scopes))
                .state(state)
                .redirectionURI(config.redirectUrl())
                .responseType(ResponseType.CODE)
                .build();
        var clientCreds = new ClientSecretBasic(clientID, clientSecret);
        var req = new PushedAuthorizationRequest(parUri, clientCreds, authzReq).toHTTPRequest();
        PushedAuthorizationResponse resp;
        try {
            var httpResp = req.send();
            resp = PushedAuthorizationResponse.parse(httpResp);
        } catch (ParseException e) {
            LOGGER.warn("Got invalid response from {}", parUri, e);
            return new ErrorParResponse(e.toString());
        } catch (IOException e) {
            LOGGER.warn("Could not reach {}", parUri, e);
            return new UnableToSendPar();
        }
        if (!resp.indicatesSuccess()) {
            return new ErrorParResponse(resp.toErrorResponse().getErrorObject().getCode());
        }
        var successResp = resp.toSuccessResponse();
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(successResp.getLifetime());
        var urn = successResp.getRequestURI();
        var redirectUri = new AuthorizationRequest.Builder(urn, clientID)
                .endpointURI(cdsServer.authorizationEndpoint())
                .build()
                .toURI();
        return new SuccessfulParResponse(redirectUri, expiresAt, state.getValue());
    }

    public AuthorizationCodeResult createAuthorizationUri(CdsServer cdsServer, List<String> scopes) {
        var clientId = new ClientID(cdsServer.clientId());
        var state = new State();
        var authUri = cdsServer.authorizationEndpoint();
        var request = new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), clientId)
                .redirectionURI(config.redirectUrl())
                .state(state)
                .scope(Scope.parse(scopes))
                .endpointURI(authUri)
                .build();

        return new AuthorizationCodeResult(request.toURI(), state.getValue());
    }

    public TokenResult retrieveAccessToken(String authCode, CdsServer cdsServer) {
        var code = new AuthorizationCode(authCode);
        var callback = config.redirectUrl();
        var codeGrant = new AuthorizationCodeGrant(code, callback);

        var clientID = new ClientID(cdsServer.clientId());
        var clientSecret = new Secret(cdsServer.clientSecret());
        var clientAuth = new ClientSecretBasic(clientID, clientSecret);

        var tokenEndpoint = cdsServer.tokenEndpoint();

        var request = new TokenRequest(tokenEndpoint, clientAuth, codeGrant);

        TokenResponse response;
        try {
            response = TokenResponse.parse(request.toHTTPRequest().send());
        } catch (Exception e) {
            LOGGER.warn("Could not parse code response", e);
            return new InvalidTokenResult();
        }

        if (!response.indicatesSuccess()) {
            var errorResponse = response.toErrorResponse();
            LOGGER.info("Could not retrieve access token {}", errorResponse.getErrorObject());
            return new InvalidTokenResult();
        }

        var successResponse = response.toSuccessResponse();
        var accessToken = successResponse.getTokens().getAccessToken();
        var refreshToken = successResponse.getTokens().getRefreshToken();
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(accessToken.getLifetime());
        return new Credentials(accessToken.getValue(), refreshToken.getValue(), expiresAt);
    }
}
