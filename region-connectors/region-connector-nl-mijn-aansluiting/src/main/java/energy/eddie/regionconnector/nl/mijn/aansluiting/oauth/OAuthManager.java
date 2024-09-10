package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.JWTProcessor;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

@Component
public class OAuthManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthManager.class);
    private final MijnAansluitingConfiguration configuration;
    private final PrivateKey privateKey;
    private final OIDCProviderMetadata providerMetadata;
    private final Map<String, List<String>> tokenRequestCustomParams;
    private final JWTProcessor<SecurityContext> jwtProcessor;
    private final OAuthTokenRepository repository;
    private final NlPermissionRequestRepository permissionRequestRepository;

    public OAuthManager(
            MijnAansluitingConfiguration configuration,
            PrivateKey privateKey,
            OIDCProviderMetadata providerMetadata,
            JWTProcessor<SecurityContext> jwtProcessor,
            OAuthTokenRepository repository,
            NlPermissionRequestRepository permissionRequestRepository
    ) {
        this.configuration = configuration;
        this.privateKey = privateKey;
        this.providerMetadata = providerMetadata;
        this.jwtProcessor = jwtProcessor;
        this.repository = repository;

        this.tokenRequestCustomParams = Map.of("client_id", List.of(configuration.clientId().getValue()));
        this.permissionRequestRepository = permissionRequestRepository;
    }

    /**
     * Creates an authorization URL.
     *
     * @param verificationCode the house number or verification code
     * @return a redirect URI
     */
    public OAuthRequestPayload createAuthorizationUrl(String verificationCode) {
        // Pass random state instead of permission id to authorization server
        State state = new State();
        CodeVerifier codeVerifier = new CodeVerifier();

        AuthorizationRequest request = new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), configuration.clientId())
                .redirectionURI(configuration.redirectUrl())
                .state(state)
                .scope(configuration.scope())
                .customParameter("verify", verificationCode)
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .endpointURI(providerMetadata.getAuthorizationEndpointURI())
                .build();

        return new OAuthRequestPayload(request.toURI(), state.getValue(), codeVerifier.getValue());
    }

    /**
     * Validates the callback uri parameters, in case they are not valid an exception is thrown. If the callback uri is
     * valid the access and/or refresh tokens are requested from the authorization server. These are persisted to the
     * database on success.
     *
     * @param callbackUri  the original callback URI containing all query parameters provided by the authorization
     *                     server.
     * @param permissionId the permission ID of the permission request associated with this callback.
     * @return the permissionId on success
     * @throws ParseException                    when the callback URI could not be parsed
     * @throws OAuthException                    when an unexpected exception is thrown during processing the callback
     * @throws PermissionNotFoundException       when the permission request the callback references does not exist
     * @throws IllegalTokenException             when the token response from the authorization server could not be
     *                                           parsed
     * @throws JWTSignatureCreationException     when the private key JWZ could not be created
     * @throws UserDeniedAuthorizationException  when the final customer did not accept the permission request
     * @throws InvalidValidationAddressException when the final customer provided an invalid address to validate the
     *                                           permission request with the permission administrator
     * @throws OAuthUnavailableException         when the authorization server is not available
     */
    public String processCallback(
            URI callbackUri,
            String permissionId
    ) throws ParseException,
             OAuthException,
             PermissionNotFoundException,
             IllegalTokenException,
             JWTSignatureCreationException,
             UserDeniedAuthorizationException,
             InvalidValidationAddressException,
             OAuthUnavailableException {
        AuthorizationResponse response = AuthorizationResponse.parse(callbackUri);

        if (!response.indicatesSuccess()) {
            // The request was denied or some error occurred
            ErrorObject errorObject = response.toErrorResponse().getErrorObject();
            if (errorObject.getCode().equalsIgnoreCase("consent_rejected")) {
                LOGGER.info("User denied authorization");
                throw new UserDeniedAuthorizationException();
            }
            if (errorObject.getCode().equalsIgnoreCase("incorrect_address")) {
                LOGGER.info("User provided an incorrect address for validation. Permission Request is invalid {}",
                            permissionId);
                throw new InvalidValidationAddressException();
            }

            LOGGER.warn("Some error occurred processing the OAuth callback: {} | {} ",
                        errorObject.getCode(),
                        errorObject.getDescription());
            throw new OAuthException("Unknown error occurred. Please contact service provider");
        }

        String state = response.getState().getValue();
        var permissionRequest = permissionRequestRepository.findByStateAndPermissionId(state, permissionId);

        if (permissionRequest.isEmpty()) {
            LOGGER.warn(
                    "No OAuth request with state {} and permissionId {} is known. This might indicate an unexpected or tampered response",
                    state,
                    permissionId
            );
            throw new PermissionNotFoundException(permissionId);
        }


        AuthorizationCode authorizationCode = response.toSuccessResponse().getAuthorizationCode();

        NlPermissionRequest nlPermissionRequest = permissionRequest.get();
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(authorizationCode,
                                                                  configuration.redirectUrl(),
                                                                  new CodeVerifier(nlPermissionRequest.codeVerifier()));
        var clientAuthentication = createSignedJwt();
        TokenRequest request = new TokenRequest(providerMetadata.getTokenEndpointURI(), clientAuthentication,
                                                codeGrant, configuration.scope(), null, tokenRequestCustomParams);
        getTokens(request, permissionId);
        return permissionId;
    }

    /**
     * Creates a signed private key JWT.
     *
     * @return a signed private key JWT
     * @throws JWTSignatureCreationException when the token could not be signed or created
     */
    private ClientAuthentication createSignedJwt() throws JWTSignatureCreationException {
        try {
            return new PrivateKeyJWT(configuration.clientId(), providerMetadata.getTokenEndpointURI(),
                                     JWSAlgorithm.RS256, privateKey, configuration.keyId(), null);
        } catch (JOSEException exception) {
            LOGGER.warn("Error while creating signed JWT", exception);
            throw new JWTSignatureCreationException();
        }
    }

    /**
     * Retrieves the access/refresh tokens from the authorization server and persists them to the database.
     *
     * @param request      the token request, can either be an initial token request or a token request with a refresh
     *                     token
     * @param permissionId the permission ID that is associated with the access/refresh tokens
     * @return the access/refresh tokens
     * @throws OAuthException            when for some reason the tokens could not be requested or persisted to the
     *                                   database
     * @throws IllegalTokenException     when the returned tokens could not be parsed
     * @throws OAuthUnavailableException when the authorization server is not available
     */
    private OAuthTokenDetails getTokens(
            TokenRequest request,
            String permissionId
    ) throws OAuthException, IllegalTokenException, OAuthUnavailableException {
        TokenResponse response;
        try {
            response = TokenResponse.parse(request.toHTTPRequest().send());
        } catch (ParseException e) {
            throw new IllegalTokenException(e);
        } catch (IOException e) {
            throw new OAuthUnavailableException(e);
        }

        if (!response.indicatesSuccess()) {
            ErrorObject errorObject = response.toErrorResponse().getErrorObject();
            LOGGER.warn("Error while exchanging authorization code for access token: {} | {}",
                        errorObject.getCode(),
                        errorObject.getDescription());
            throw new OAuthException("Error while exchanging authorization code for access token");
        }
        var tokens = response.toSuccessResponse().getTokens();
        // verify token signature & claims
        try {
            var signedJWT = SignedJWT.parse(tokens.getAccessToken().getValue());
            var claimsSet = jwtProcessor.process(signedJWT, null);
            return saveTokensInDb(permissionId, tokens, claimsSet);
        } catch (BadJOSEException | java.text.ParseException | JOSEException e) {
            LOGGER.error("Received invalid token for permissionId {}", permissionId, e);
            throw new OAuthException("Failed to get a valid token from the OAuth authorization server.");
        }
    }

    private OAuthTokenDetails saveTokensInDb(
            String permissionId,
            Tokens tokens,
            JWTClaimsSet claimsSet
    ) {
        var refreshToken = tokens.getRefreshToken() == null ? null : tokens.getRefreshToken().getValue();
        OAuthTokenDetails oAuthTokenDetails = new OAuthTokenDetails(permissionId,
                                                                    tokens.getAccessToken().getValue(),
                                                                    claimsSet.getIssueTime().toInstant(),
                                                                    claimsSet.getExpirationTime().toInstant(),
                                                                    refreshToken,
                                                                    claimsSet.getIssueTime().toInstant());
        return repository.save(oAuthTokenDetails);
    }

    /**
     * Returns a valid access token and the endpoint to request the data from
     *
     * @param permissionId the permission ID associated with the tokens
     * @return an access token and the endpoint to request data from
     * @throws OAuthException                a general exception in case of an unexpected error
     * @throws JWTSignatureCreationException when the private key JWT could not be created
     * @throws IllegalTokenException         when the token response could not be parsed
     * @throws NoRefreshTokenException       when the permission request does not contain a refresh token and the access
     *                                       token cannot be refreshed
     * @throws OAuthUnavailableException     when the OAuth server is not available
     */
    public AccessTokenAndSingleSyncUrl accessTokenAndSingleSyncUrl(String permissionId)
            throws OAuthException, JWTSignatureCreationException, IllegalTokenException, NoRefreshTokenException, OAuthUnavailableException {
        var credentials = repository.findById(permissionId)
                                    .orElseThrow(() -> new OAuthTokenDetailsNotFoundException(permissionId));
        String accessToken;
        if (credentials.isValid()) {
            accessToken = credentials.accessToken();
        } else {
            if (credentials.refreshToken() == null) {
                throw new NoRefreshTokenException();
            }
            var refreshTokenGrant = new RefreshTokenGrant(new RefreshToken(credentials.refreshToken()));
            var clientAuthentication = createSignedJwt();
            TokenRequest request = new TokenRequest(providerMetadata.getTokenEndpointURI(),
                                                    clientAuthentication,
                                                    refreshTokenGrant,
                                                    configuration.scope(),
                                                    null,
                                                    tokenRequestCustomParams);

            accessToken = getTokens(request, permissionId).accessToken();
        }
        try {
            var claimsSet = jwtProcessor.process(SignedJWT.parse(accessToken), null);
            var singleSync = getSingleSyncUrl(claimsSet);
            return new AccessTokenAndSingleSyncUrl(accessToken, singleSync);
        } catch (java.text.ParseException | BadJOSEException | JOSEException e) {
            throw new OAuthException("Unable to process access token for permission request %s permission id.".formatted(
                    permissionId));
        }
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    private static String getSingleSyncUrl(JWTClaimsSet claimsSet) {
        Map<String, Object> resources = (Map<String, Object>) ((List<Object>) claimsSet.getClaim("resources")).getFirst();
        return ((Map<String, String>) resources.get("endpoints")).get("single_sync");
    }
}
