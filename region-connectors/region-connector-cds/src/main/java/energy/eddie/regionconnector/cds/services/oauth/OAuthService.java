package energy.eddie.regionconnector.cds.services.oauth;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.services.oauth.code.AuthorizationCodeResult;
import energy.eddie.regionconnector.cds.services.oauth.par.ErrorParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.ParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
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

    public OAuthService(CdsConfiguration config) {
        this.config = config;
    }

    // TODO: Use correct credentials to push authorization request
    public ParResponse pushAuthorization(
            CdsServer cdsServer,
            List<String> scopes
    ) {
        var clientID = new ClientID(cdsServer.adminClientId());
        var clientSecret = new Secret(cdsServer.adminClientSecret());
        var state = new State();
        var parUri = cdsServer.endpoints().pushedAuthorizationRequestEndpoint();
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
        var redirectUri = new AuthorizationRequest.Builder(urn, new ClientID(cdsServer.adminClientId()))
                .endpointURI(cdsServer.endpoints().authorizationEndpoint())
                .build()
                .toURI();
        return new SuccessfulParResponse(redirectUri, expiresAt, state.getValue());
    }

    public AuthorizationCodeResult createAuthorizationUri(
            CdsServer cdsServer,
            List<String> scopes
    ) {
        var clientId = new ClientID(cdsServer.customerDataClientId());
        var state = new State();
        var authUri = cdsServer.endpoints().authorizationEndpoint();
        var request = new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), clientId)
                .redirectionURI(config.redirectUrl())
                .state(state)
                .scope(Scope.parse(scopes))
                .endpointURI(authUri)
                .build();

        return new AuthorizationCodeResult(request.toURI(), state.getValue());
    }

    /**
     * Retrieves the access token for a user, using the code flow
     *
     * @param authCode  auth code that was part of the redirect URI
     * @param cdsServer the cds server that the user belongs to
     * @return the result of the token request
     */
    // TODO: Use correct credentials to request token
    public TokenResult retrieveAccessToken(
            String authCode,
            CdsServer cdsServer
    ) {
        var code = new AuthorizationCode(authCode);
        var callback = config.redirectUrl();
        var codeGrant = new AuthorizationCodeGrant(code, callback);
        var clientID = new ClientID(cdsServer.customerDataClientId());

        var tokenEndpoint = cdsServer.endpoints().tokenEndpoint();

        var request = new TokenRequest(tokenEndpoint, clientID, codeGrant);

        return sendAccessTokenRequest(request);
    }

    /**
     * Retrieves an admin client for a CDS server to get access to admin APIs
     *
     * @param cdsServer the cds server that is used
     * @return the result of the token request
     */
    public TokenResult retrieveAccessToken(CdsServer cdsServer) {
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        var clientID = new ClientID(cdsServer.adminClientId());
        var clientSecret = new Secret(cdsServer.adminClientSecret());
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);
        var scope = new Scope(Scopes.CLIENT_ADMIN_SCOPE);

        var tokenEndpoint = cdsServer.endpoints().tokenEndpoint();
        var request = new TokenRequest(tokenEndpoint, clientAuth, clientGrant, scope);
        return sendAccessTokenRequest(request);
    }

    private TokenResult sendAccessTokenRequest(TokenRequest request) {
        TokenResponse response;
        try {
            var httpRequest = request.toHTTPRequest();
            response = TokenResponse.parse(httpRequest.send());
        } catch (Exception e) {
            LOGGER.warn("Could not parse code response", e);
            return new InvalidTokenResult();
        }

        if (!response.indicatesSuccess()) {
            var errorResponse = response.toErrorResponse();
            LOGGER.info("Could not retrieve access token, response had status {} and error object {}",
                        errorResponse.getErrorObject().getHTTPStatusCode(),
                        errorResponse.getErrorObject());
            return new InvalidTokenResult();
        }

        var successResponse = response.toSuccessResponse();
        var accessToken = successResponse.getTokens().getAccessToken();
        var refreshToken = successResponse.getTokens().getRefreshToken();
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(accessToken.getLifetime());
        if (refreshToken == null) {
            return new CredentialsWithoutRefreshToken(accessToken.getValue(), expiresAt);
        }
        return new CredentialsWithRefreshToken(accessToken.getValue(), refreshToken.getValue(), expiresAt);
    }
}
