package energy.eddie.regionconnector.cds.client;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClient;
import energy.eddie.regionconnector.cds.dtos.CdsServerRedirectUriUpdate;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.code.AuthorizationCodeResult;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.cds.services.oauth.revocation.RevocationResult;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsServerClientTest {
    @Spy
    @SuppressWarnings("unused")
    private final CdsServer cdsServer = new CdsServerBuilder()
            .setId(1L)
            .setBaseUri("http://localhost")
            .setAdminClientId("client-id")
            .setAdminClientSecret("client-secret")
            .build();
    @Mock
    private CdsPublicApis publicApis;
    @Mock
    private AdminClient adminClient;
    @Mock
    private CustomerDataClient customerDataClient;
    @Mock
    private OAuthService oAuthService;
    @Mock
    private CustomerDataTokenService tokenService;
    @InjectMocks
    private CdsServerClient client;

    @Test
    void testPushAuthorizationRequest_returnsResult() {
        // Given
        var uri = URI.create("http://localhost");
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var token = new CredentialsWithoutRefreshToken("access-token", now.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        when(oAuthService.pushAuthorization(List.of(Scopes.CUSTOMER_DATA_SCOPE),
                                            "customer-client-id",
                                            "customer-client-secret",
                                            uri,
                                            uri))
                .thenReturn(new SuccessfulParResponse(uri, now, "state"));
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        when(adminClient.credentials("customer-client-id", uri, token))
                .thenReturn(Mono.just(
                        new ListingCredentials200Response()
                                .addCredentialsItem(
                                        new ListingCredentials200ResponseCredentialsInner()
                                                .clientId("customer-client-id")
                                                .clientSecret("customer-client-secret")
                                )
                ));
        when(adminClient.carbonDataSpec(uri, token)).thenReturn(Mono.just(cdsData));
        when(adminClient.oauthMetadataSpec(uri, token)).thenReturn(Mono.just(oauthMetadata));

        // When
        var res = client.pushAuthorizationRequest(List.of(Scopes.CUSTOMER_DATA_SCOPE));

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(SuccessfulParResponse.class))
                .satisfies(success -> {
                    assertThat(success.redirectUri()).isEqualTo(uri);
                    assertThat(success.expiresAt()).isEqualTo(now);
                    assertThat(success.state()).isEqualTo("state");
                });
    }

    @Test
    void testPushAuthorizationRequest_withEmptyStreams_returnsUnableToSendParResult() {
        // Given
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.empty());

        // When
        var res = client.pushAuthorizationRequest(List.of(Scopes.CUSTOMER_DATA_SCOPE));

        // Then
        assertThat(res).isInstanceOf(UnableToSendPar.class);
    }

    @Test
    void testPushAuthorizationRequest_withInvalidToken_returnsUnableToSendParResult() {
        // Given
        var uri = URI.create("http://localhost");
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.just(createCdsMetadataResponse(uri)));
        when(publicApis.oauthMetadataSpec(any())).thenReturn(Mono.just(createOAuthMetadataResponse(uri)));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri))
                .thenReturn(new InvalidTokenResult());

        // When
        var res = client.pushAuthorizationRequest(List.of(Scopes.CUSTOMER_DATA_SCOPE));

        // Then
        assertThat(res).isInstanceOf(UnableToSendPar.class);
    }

    @Test
    void testCreateAuthorizationUri_returnsResult() {
        // Given
        var uri = URI.create("http://localhost");
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var token = new CredentialsWithoutRefreshToken("access-token", now.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        when(oAuthService.createAuthorizationUri(List.of(Scopes.CUSTOMER_DATA_SCOPE), "customer-client-id", uri))
                .thenReturn(new AuthorizationCodeResult(uri, "state"));
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        when(adminClient.carbonDataSpec(uri, token)).thenReturn(Mono.just(cdsData));
        when(adminClient.oauthMetadataSpec(uri, token)).thenReturn(Mono.just(oauthMetadata));

        // When
        var res = client.createAuthorizationUri(List.of(Scopes.CUSTOMER_DATA_SCOPE));

        // Then
        assertThat(res)
                .isPresent()
                .get()
                .satisfies(codeResult -> {
                    assertThat(codeResult.redirectUri()).isEqualTo(uri);
                    assertThat(codeResult.state()).isEqualTo("state");
                });
    }

    @Test
    void testCreateAuthorizationUri_withInvalidToken_returnsEmptyOptional() {
        // Given
        var uri = URI.create("http://localhost");
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(createCdsMetadataResponse(uri)));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(createOAuthMetadataResponse(uri)));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri))
                .thenReturn(new InvalidTokenResult());

        // When
        var res = client.createAuthorizationUri(List.of(Scopes.CUSTOMER_DATA_SCOPE));

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void testRetrieveCustomerCredentials_returnsResult() {
        // Given
        var uri = URI.create("http://localhost");
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var token = new CredentialsWithoutRefreshToken("access-token", now.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        when(oAuthService.retrieveAccessToken("code", "customer-client-id", "customer-client-secret", uri))
                .thenReturn(new CredentialsWithRefreshToken("access-token", "refresh-token", now));
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        when(adminClient.credentials("customer-client-id", uri, token))
                .thenReturn(Mono.just(
                        new ListingCredentials200Response()
                                .addCredentialsItem(
                                        new ListingCredentials200ResponseCredentialsInner()
                                                .clientId("customer-client-id")
                                                .clientSecret("customer-client-secret")
                                )
                ));

        // When
        var res = client.retrieveCustomerCredentials("code");

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(CredentialsWithRefreshToken.class))
                .satisfies(success -> {
                    assertThat(success.accessToken()).isEqualTo("access-token");
                    assertThat(success.refreshToken()).isEqualTo("refresh-token");
                    assertThat(success.expiresAt()).isEqualTo(now);
                });
    }

    @Test
    void testRetrieveCustomerCredentials_withoutCustomerDataClient_returnsInvalidTokenResult() {
        // Given
        var uri = URI.create("http://localhost");
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var token = new CredentialsWithoutRefreshToken("access-token", now.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CLIENT_ADMIN_SCOPE, "client-id")));

        // When
        var res = client.retrieveCustomerCredentials("code");

        // Then
        assertThat(res).isInstanceOf(InvalidTokenResult.class);
    }

    @Test
    void testRetrieveCustomerCredentials_whereClientCredentialsCouldNotBeRetrieved_returnsInvalidResult() {
        // Given
        var uri = URI.create("http://localhost");
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri))
                .thenReturn(new InvalidTokenResult());
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));

        // When
        var res = client.retrieveCustomerCredentials("code");

        // Then
        assertThat(res)
                .isInstanceOf(InvalidTokenResult.class);
    }

    @Test
    void testRetrieveCustomerCredentials_withUnusualToken_returnsResult() {
        // Given
        var uri = URI.create("http://localhost");
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var token = new CredentialsWithRefreshToken("access-token", "refresh-token", now.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        when(oAuthService.retrieveAccessToken("code", "customer-client-id", "customer-client-secret", uri))
                .thenReturn(new CredentialsWithRefreshToken("access-token", "refresh-token", now));
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(adminClient.clients(eq(uri), any()))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        when(adminClient.credentials(eq("customer-client-id"), eq(uri), any()))
                .thenReturn(Mono.just(
                        new ListingCredentials200Response()
                                .addCredentialsItem(
                                        new ListingCredentials200ResponseCredentialsInner()
                                                .clientId("customer-client-id")
                                                .clientSecret("customer-client-secret")
                                )
                ));

        // When
        var res = client.retrieveCustomerCredentials("code");

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(CredentialsWithRefreshToken.class))
                .satisfies(success -> {
                    assertThat(success.accessToken()).isEqualTo("access-token");
                    assertThat(success.refreshToken()).isEqualTo("refresh-token");
                    assertThat(success.expiresAt()).isEqualTo(now);
                });
    }

    @Test
    void testRetrieveCustomerCredentials_withEmptyStream_returnsInvalidTokenResult() {
        // Given
        when(publicApis.carbonDataSpec(any())).thenReturn(Mono.empty());

        // When
        var res = client.retrieveCustomerCredentials("code");

        // Then
        assertThat(res).isInstanceOf(InvalidTokenResult.class);
    }

    @Test
    void testModifyClientWithScope_returnsVoidMono() {
        // Given
        var uri = URI.create("http://localhost");
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);
        var token = new CredentialsWithoutRefreshToken("access-token", expiresAt.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        var update = new CdsServerRedirectUriUpdate(List.of(uri));
        when(adminClient.modifyClient(update, uri, token))
                .thenReturn(Mono.just(new RetrievingIndividualClients200Response()));

        // When
        var res = client.modifyClientWithScope(Scopes.CUSTOMER_DATA_SCOPE,
                                               update);

        // Then
        StepVerifier.create(res)
                    .verifyComplete();
    }

    @Test
    void testRevokeToken_returnsRevocationResult() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var oAuthCredentials = new OAuthCredentials("pid", null, null, null);
        var uri = URI.create("http://localhost");
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);
        var token = new CredentialsWithoutRefreshToken("access-token", expiresAt.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        when(oAuthService.revokeToken(uri, oAuthCredentials, "customer-client-id", "customer-client-secret"))
                .thenReturn(new RevocationResult.SuccessfulRevocation());
        when(tokenService.getOAuthCredentialsAsync(eq("pid"), any()))
                .thenReturn(Mono.just(oAuthCredentials));
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        when(adminClient.credentials("customer-client-id", uri, token))
                .thenReturn(Mono.just(
                        new ListingCredentials200Response()
                                .addCredentialsItem(
                                        new ListingCredentials200ResponseCredentialsInner()
                                                .clientId("customer-client-id")
                                                .clientSecret("customer-client-secret")
                                )
                ));

        // When
        var res = client.revokeToken(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(rev -> assertThat(rev)
                            .isInstanceOf(RevocationResult.SuccessfulRevocation.class))
                    .verifyComplete();
    }

    @Test
    void testRevokeToken_withMissingTokens_returnsServiceUnavailable() {
        // Given
        var pr = new CdsPermissionRequestBuilder().build();
        var uri = URI.create("http://localhost");
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri))
                .thenReturn(new InvalidTokenResult());
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(createCdsMetadataResponse(uri)));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(createOAuthMetadataResponse(uri)));

        // When
        var res = client.revokeToken(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(rev -> assertThat(rev)
                            .isInstanceOf(RevocationResult.ServiceUnavailable.class))
                    .verifyComplete();
    }

    @Test
    void testAccountingPointData_returnsAccountingPointData() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var oAuthCredentials = new OAuthCredentials("pid", null, null, null);
        var uri = URI.create("http://localhost");
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);
        var token = new CredentialsWithoutRefreshToken("access-token", expiresAt.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        when(tokenService.getOAuthCredentialsAsync(eq("pid"), any()))
                .thenReturn(Mono.just(oAuthCredentials));
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(customerDataClient.accounts(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.serviceContracts(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.servicePoints(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.meterDevices(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        when(adminClient.credentials("customer-client-id", uri, token))
                .thenReturn(Mono.just(
                        new ListingCredentials200Response()
                                .addCredentialsItem(
                                        new ListingCredentials200ResponseCredentialsInner()
                                                .clientId("customer-client-id")
                                                .clientSecret("customer-client-secret")
                                )
                ));

        // When
        var res = client.accountingPointData(pr);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testValidatedHistoricalData_returnsValidatedHistoricalData() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var oAuthCredentials = new OAuthCredentials("pid", null, null, null);
        var uri = URI.create("http://localhost");
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var expiresAt = now.plusDays(10);
        var token = new CredentialsWithoutRefreshToken("access-token", expiresAt.plusDays(1));
        when(oAuthService.retrieveAccessToken("client-id", "client-secret", uri)).thenReturn(token);
        when(tokenService.getOAuthCredentialsAsync(eq("pid"), any()))
                .thenReturn(Mono.just(oAuthCredentials));
        var cdsData = createCdsMetadataResponse(uri);
        var oauthMetadata = createOAuthMetadataResponse(uri);
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(cdsData));
        when(publicApis.oauthMetadataSpec(uri)).thenReturn(Mono.just(oauthMetadata));
        when(customerDataClient.accounts(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.serviceContracts(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.servicePoints(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.meterDevices(uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(customerDataClient.usageSegments(now, now, uri, oAuthCredentials)).thenReturn(Mono.just(List.of()));
        when(adminClient.clients(uri, token))
                .thenReturn(Mono.just(createClient(uri, Scopes.CUSTOMER_DATA_SCOPE, "customer-client-id")));
        when(adminClient.credentials("customer-client-id", uri, token))
                .thenReturn(Mono.just(
                        new ListingCredentials200Response()
                                .addCredentialsItem(
                                        new ListingCredentials200ResponseCredentialsInner()
                                                .clientId("customer-client-id")
                                                .clientSecret("customer-client-secret")
                                )
                ));

        // When
        var res = client.validatedHistoricalData(pr, now, now);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testMasterData_returnsMasterData() {
        // Given
        var uri = URI.create("http://localhost");
        when(publicApis.carbonDataSpec(uri)).thenReturn(Mono.just(createCdsMetadataResponse(uri)));
        when(publicApis.coverage(uri)).thenReturn(Mono.just(
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(List.of(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.ELECTRICITY))
                                .country("us")
                )
        ));

        // When
        var res = client.masterData();

        // Then
        StepVerifier.create(res)
                    .assertNext(data -> assertThat(data)
                            .satisfies(masterData -> {
                                assertThat(masterData.baseUri()).isEqualTo(uri);
                                assertThat(masterData.id()).isEqualTo("1");
                                assertThat(masterData.name()).isEqualTo("CDS Server");
                                assertThat(masterData.countries()).containsExactly("us");
                                assertThat(masterData.energyTypes()).containsExactly(EnergyType.ELECTRICITY);
                            }))
                    .verifyComplete();
    }

    private static List<ClientEndpoint200ResponseClientsInner> createClient(URI uri, String scope, String clientId) {
        return List.of(
                new ClientEndpoint200ResponseClientsInner()
                        .scope(scope)
                        .clientId(clientId)
                        .cdsServerMetadata(uri)
                        .cdsClientUri(uri)
        );
    }

    private static CarbonDataSpec200Response createCdsMetadataResponse(URI uri) {
        return new CarbonDataSpec200Response()
                .name("CDS Server")
                .coverage(uri)
                .oauthMetadata(uri);
    }

    private static OAuthAuthorizationServer200Response createOAuthMetadataResponse(URI uri) {
        return new OAuthAuthorizationServer200Response()
                .cdsClientsApi(uri)
                .tokenEndpoint(uri)
                .cdsCredentialsApi(uri)
                .pushedAuthorizationRequestEndpoint(uri)
                .revocationEndpoint(uri)
                .authorizationEndpoint(uri)
                .cdsCustomerdataAccountsApi(uri)
                .cdsCustomerdataServicecontractsApi(uri)
                .cdsCustomerdataServicepointsApi(uri)
                .cdsCustomerdataMeterdevicesApi(uri)
                .cdsCustomerdataUsagesegmentsApi(uri);
    }
}