package energy.eddie.regionconnector.cds.client;

import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClient;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import energy.eddie.regionconnector.cds.dtos.CdsServerRedirectUriUpdate;
import energy.eddie.regionconnector.cds.exceptions.NoCustomerDataClientFoundException;
import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.code.AuthorizationCodeResult;
import energy.eddie.regionconnector.cds.services.oauth.par.ParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.cds.services.oauth.revocation.RevocationResult;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import energy.eddie.regionconnector.cds.services.oauth.token.TokenResult;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuple5;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class CdsServerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsServerClient.class);
    private final CdsServer cdsServer;
    private final CdsPublicApis publicApis;
    private final AdminClient adminClient;
    private final CustomerDataClient customerDataClient;
    private final OAuthService oAuthService;
    private final CustomerDataTokenService customerDataTokenService;
    @Nullable
    private CredentialsWithoutRefreshToken cachedToken = null;

    public CdsServerClient(
            CdsServer cdsServer,
            CdsPublicApis publicApis,
            AdminClient adminClient,
            CustomerDataClient customerDataClient,
            OAuthService oAuthService,
            CustomerDataTokenService customerDataTokenService
    ) {
        this.cdsServer = cdsServer;
        this.publicApis = publicApis;
        this.adminClient = adminClient;
        this.customerDataClient = customerDataClient;
        this.oAuthService = oAuthService;
        this.customerDataTokenService = customerDataTokenService;
    }

    public ParResponse pushAuthorizationRequest(List<String> scopes) {
        try {
            return findClientByScope(Scopes.CUSTOMER_DATA_SCOPE)
                    .flatMap(response -> Mono.zip(
                            customerDataClientCredentials(),
                            oAuthMetadata(response.getCdsServerMetadata())
                    ))
                    .blockOptional()
                    .map(response ->
                                 oAuthService.pushAuthorization(
                                         scopes,
                                         response.getT1().clientId(),
                                         response.getT1().clientSecret(),
                                         response.getT2().getPushedAuthorizationRequestEndpoint(),
                                         response.getT2().getAuthorizationEndpoint()
                                 ))
                    .orElse(new UnableToSendPar());
        } catch (RuntimeException e) {
            LOGGER.warn("Got an error while requesting token", e);
            return new UnableToSendPar();
        }
    }

    public Optional<AuthorizationCodeResult> createAuthorizationUri(List<String> scopes) {
        try {
            return findClientByScope(Scopes.CUSTOMER_DATA_SCOPE)
                    .flatMap(response -> Mono.zip(Mono.just(response),
                                                  oAuthMetadata(response.getCdsServerMetadata())))
                    .blockOptional()
                    .map(response -> oAuthService.createAuthorizationUri(
                            scopes,
                            response.getT1().getClientId(),
                            response.getT2().getAuthorizationEndpoint()
                    ));
        } catch (RuntimeException e) {
            LOGGER.warn("Got an error while requesting token", e);
            return Optional.empty();
        }
    }

    public TokenResult retrieveCustomerCredentials(String code) {
        return customerDataClientCredentials()
                .map(response -> oAuthService.retrieveAccessToken(
                        code,
                        response.clientId(),
                        response.clientSecret(),
                        response.tokenEndpoint()
                ))
                .onErrorResume(CdsServerClient::isMissingCredentials, e -> {
                    LOGGER.warn("Got error while requesting client credentials", e);
                    return Mono.just(new InvalidTokenResult());
                })
                .defaultIfEmpty(new InvalidTokenResult())
                .block();
    }

    public Mono<Void> modifyClientWithScope(String scope, CdsServerRedirectUriUpdate update) {
        return findClientByScope(scope)
                .flatMap(this::withRefreshToken)
                .flatMap(clientAndToken -> adminClient.modifyClient(
                        update,
                        clientAndToken.getT1().getCdsClientUri(),
                        clientAndToken.getT2()
                ))
                .then();
    }

    public Mono<RevocationResult> revokeToken(CdsPermissionRequest permissionRequest) {
        return withRefreshToken(oAuthMetadata(), permissionRequest)
                .flatMap(response -> Mono.zip(
                        Mono.just(response.getT1()),
                        Mono.just(response.getT2()),
                        customerDataClientCredentials()
                ))
                .map(response -> oAuthService.revokeToken(
                        response.getT1().getRevocationEndpoint(),
                        response.getT2(),
                        response.getT3().clientId(),
                        response.getT3().clientSecret()
                ))
                .onErrorResume(CdsServerClient::isMissingCredentials,
                               e -> {
                                   LOGGER.warn("Got an error while revoking token", e);
                                   return Mono.just(new RevocationResult.ServiceUnavailable());
                               });
    }

    public Mono<Tuple4<
            List<AccountsEndpoint200ResponseAllOfAccountsInner>,
            List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner>,
            List<ServicePointEndpoint200ResponseAllOfServicePointsInner>,
            List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner>
            >> accountingPointData(CdsPermissionRequest permissionRequest) {
        return withRefreshToken(findClientByScope(Scopes.CUSTOMER_DATA_SCOPE), permissionRequest)
                .flatMap(res -> Mono.zip(
                        oAuthMetadata(res.getT1().getCdsServerMetadata()),
                        Mono.just(res.getT2())
                ))
                .flatMap(response -> Mono.zip(
                        customerDataClient.accounts(response.getT1().getCdsCustomerdataAccountsApi(), response.getT2()),
                        customerDataClient.serviceContracts(response.getT1().getCdsCustomerdataServicecontractsApi(),
                                                            response.getT2()),
                        customerDataClient.servicePoints(response.getT1().getCdsCustomerdataServicepointsApi(),
                                                         response.getT2()),
                        customerDataClient.meterDevices(response.getT1().getCdsCustomerdataMeterdevicesApi(),
                                                        response.getT2())
                ));
    }

    public Mono<Tuple5<
            List<AccountsEndpoint200ResponseAllOfAccountsInner>,
            List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner>,
            List<ServicePointEndpoint200ResponseAllOfServicePointsInner>,
            List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner>,
            List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>
            >> validatedHistoricalData(
            CdsPermissionRequest permissionRequest,
            ZonedDateTime before,
            ZonedDateTime after
    ) {
        return withRefreshToken(findClientByScope(Scopes.CUSTOMER_DATA_SCOPE), permissionRequest)
                .flatMap(res -> Mono.zip(
                        oAuthMetadata(res.getT1().getCdsServerMetadata()),
                        Mono.just(res.getT2())
                ))
                .flatMap(response -> Mono.zip(
                        customerDataClient.accounts(response.getT1().getCdsCustomerdataAccountsApi(),
                                                    response.getT2()),
                        customerDataClient.serviceContracts(response.getT1()
                                                                    .getCdsCustomerdataServicecontractsApi(),
                                                            response.getT2()),
                        customerDataClient.servicePoints(response.getT1().getCdsCustomerdataServicepointsApi(),
                                                         response.getT2()),
                        customerDataClient.meterDevices(response.getT1().getCdsCustomerdataMeterdevicesApi(),
                                                        response.getT2()),
                        customerDataClient.usageSegments(before,
                                                         after,
                                                         response.getT1().getCdsCustomerdataUsagesegmentsApi(),
                                                         response.getT2())
                ));
    }

    public Mono<CdsServerMasterData> masterData() {
        return carbonDataSpecification()
                .flatMap(response -> Mono.zip(
                        Mono.just(response),
                        publicApis.coverage(response.getCoverage())
                ))
                .map(response -> new CdsServerMasterData(
                        response.getT1().getName(),
                        cdsServer.id().toString(),
                        cdsServer.baseUri(),
                        new CoverageTypes(response.getT2()).toCoverages()
                ));
    }

    private Mono<CustomerDataClientCredentials> customerDataClientCredentials() {
        return withRefreshToken(oAuthMetadata())
                .flatMap(response -> Mono.zip(
                        Mono.just(response.getT1()),
                        findClientByScope(Scopes.CUSTOMER_DATA_SCOPE),
                        Mono.just(response.getT2())
                ))
                .flatMap(response -> Mono.zip(
                        Mono.just(response.getT1()),
                        adminClient.credentials(response.getT2().getClientId(),
                                                response.getT1().getCdsCredentialsApi(),
                                                response.getT3())
                ))
                .map(CdsServerClient::toCustomerDataCredentials);
    }

    private Mono<CarbonDataSpec200Response> carbonDataSpecification() {
        return publicApis.carbonDataSpec(cdsServer.baseUri());
    }

    private Mono<OAuthAuthorizationServer200Response> oAuthMetadata() {
        return carbonDataSpecification()
                .flatMap(response -> publicApis.oauthMetadataSpec(response.getOauthMetadata()));
    }

    private Mono<OAuthAuthorizationServer200Response> oAuthMetadata(URI carbonDataSpecEndpoint) {
        return withRefreshToken(carbonDataSpecification(carbonDataSpecEndpoint))
                .flatMap(response -> adminClient.oauthMetadataSpec(
                        response.getT1().getOauthMetadata(),
                        response.getT2()
                ));
    }

    private Mono<CarbonDataSpec200Response> carbonDataSpecification(URI carbonDataSpecEndpoint) {
        return refreshTokenAsync()
                .flatMap(token -> adminClient.carbonDataSpec(carbonDataSpecEndpoint, token));
    }

    private Mono<ClientEndpoint200ResponseClientsInner> findClientByScope(String scope) {
        return clients()
                .flatMap(clients -> findClientByScope(clients, scope))
                .onErrorResume(NoCustomerDataClientFoundException.class, ex -> {
                    LOGGER.warn("Couldn't find client credentials for customer data apis", ex);
                    return Mono.empty();
                });
    }

    private Mono<List<ClientEndpoint200ResponseClientsInner>> clients() {
        return withRefreshToken(oAuthMetadata())
                .flatMap(response -> adminClient.clients(response.getT1().getCdsClientsApi(), response.getT2()));
    }

    private Mono<ClientEndpoint200ResponseClientsInner> findClientByScope(
            List<ClientEndpoint200ResponseClientsInner> response,
            String scope
    ) {
        for (var result : response) {
            if (result.getScope().contains(scope)) {
                return Mono.just(result);
            }
        }
        return Mono.error(new NoCustomerDataClientFoundException());
    }

    private <T> Mono<Tuple2<T, CredentialsWithoutRefreshToken>> withRefreshToken(Mono<T> payload) {
        return Mono.zip(payload, refreshTokenAsync());
    }

    private <T> Mono<Tuple2<T, CredentialsWithoutRefreshToken>> withRefreshToken(T payload) {
        return withRefreshToken(Mono.just(payload));
    }

    private <T> Mono<Tuple2<T, OAuthCredentials>> withRefreshToken(
            Mono<T> payload,
            CdsPermissionRequest permissionRequest
    ) {
        return customerDataClientCredentials()
                .flatMap(response -> Mono.zip(
                        payload,
                        customerDataTokenService.getOAuthCredentialsAsync(permissionRequest.permissionId(),
                                                                          response))
                );
    }

    private Mono<CredentialsWithoutRefreshToken> refreshTokenAsync() {
        return oAuthMetadata()
                .flatMap(resp -> refreshTokenAsync(resp.getTokenEndpoint()));
    }

    private Mono<CredentialsWithoutRefreshToken> refreshTokenAsync(URI tokenEndpoint) {
        return Mono.create(sink -> {
            try {
                var res = refreshToken(tokenEndpoint);
                sink.success(res);
            } catch (NoTokenException e) {
                sink.error(e);
            }
        });
    }

    private CredentialsWithoutRefreshToken refreshToken(URI tokenEndpoint) throws NoTokenException {
        if (cachedToken != null && cachedToken.isValid()) {
            return cachedToken;
        }
        var res = oAuthService.retrieveAccessToken(cdsServer.adminClientId(),
                                                   cdsServer.adminClientSecret(),
                                                   tokenEndpoint);
        cachedToken = switch (res) {
            case CredentialsWithoutRefreshToken withoutRefreshToken -> withoutRefreshToken;
            case CredentialsWithRefreshToken(
                    String accessToken, String ignoredRefreshToken, ZonedDateTime expiresAt
            ) -> {
                LOGGER.warn("Got unexpected token response with refresh token");
                yield new CredentialsWithoutRefreshToken(accessToken, expiresAt);
            }
            case InvalidTokenResult ignored -> throw new NoTokenException();
        };
        return cachedToken;
    }

    private static CustomerDataClientCredentials toCustomerDataCredentials(Tuple2<OAuthAuthorizationServer200Response, ListingCredentials200Response> credentials) {
        var customerCreds = credentials.getT2().getCredentials().getFirst();
        return new CustomerDataClientCredentials(customerCreds.getClientId(),
                                                 customerCreds.getClientSecret(),
                                                 credentials.getT1().getTokenEndpoint());
    }

    private static boolean isMissingCredentials(Throwable t) {
        return t instanceof NoTokenException || t instanceof NoCustomerDataClientFoundException;
    }
}
