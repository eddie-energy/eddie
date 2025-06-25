package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.JWTProcessor;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("JavaUtilDate") // The nimbus library still uses the Date class, which should be avoided
class OAuthManagerTest {
    private static final String PRIVATE_KEY =
            """
                    MIIG/QIBADANBgkqhkiG9w0BAQEFAASCBucwggbjAgEAAoIBgQDJfNnt6mHorE/w
                    898Dqwqu8an7dNE7A+EQ32eu+8DArom6Z4nqxr8leMD8XkLPXNLYR9Nsa4Ob4oH0
                    yc9AOi5iF2vXDOGdsb2zEsA49sCdUVQ/ZaPeU0ZWZJqIeH6eBlpQbWvfJv3H4upa
                    TW+9eHd5YtMUaJYXgVT2sXyy6jvzwkcOQO9SRv1N+VYHZpfZCR3md3rFmm0loRyu
                    KeaF8oEI/6FRzaW9/PKmkfp606lhP6q94JdF1CdrJUuVjc0kl7ATgIDHvvvyXgLL
                    ijUKATrJw0labNB4BD35uUtX0QLAp5Giy0q77wOJsZDl0VU5oDEm44VsLjvTfbzZ
                    PqSt4G552xyy83VQvRWUMs+vctt3aBnDPem5YtPz4WbFrOg4FuKKPdmHNGcA+xY5
                    HgVq0nuFmfY58yUyGvxWHJ0Axr+P3vHSj1Rb5uaQdFQhm3ezRE7tsK+ernW1qr1i
                    2VPH6nc/F+tRoqN+GQROrlLfprl01CIppxv4eKeVuD2hwMUkRbcCAwEAAQKCAYAV
                    f9X9NgJGgoRxMIR+brTQIAdERp3Az1Qnqb71g/V4WedmVX/45kYlKHJnO2OjnsL8
                    wwVDT2GWs5+sUBBmSQ8D9n3ST5SPcBTO09Rg/6tj3fbAYQcQRRB/TXLT1w+jqwfe
                    Bx74i7+OoZ7iDds7e2w8LphZWCOT1IANZrjommfVRcixM6qVu+9xb4aHeaIATaG8
                    Ismd5jF5T2yxBNWNO2gbXrOG+AuUbzYxIeh5TLaJIvMEGb/qeo3OOAeNqLjYC7tU
                    bAa7WBxvvvmyWMWFpjB8j4Cv5XQyA+D5TLajc+OAtRHPod9MdJPnO8Fe6XZdz2pZ
                    rLT0TNCNxAuC700vaxinjVGOs8uE+xlGAN3MzoTRKC99Iscj296jyCwZQnk4Kyan
                    q3iW+qr6BXs59wHoArFt5sT/ych5c6J9PA9Uarl9WiGzL9ys3dbwr1UNLaIuefhU
                    q3RKilc8T3upofjDBbYyKAyj6U+6fO5qmcw7WpPmvnB6qoqL5p5bGopBraeeztEC
                    gcEA9XdqfiddheCEsAeZOvFPdirQiB0L2egaSe2Y+YsV0BLAg3XYjGpYDGDblR5C
                    KrqAhmCVT6ED9iOu7v7sHZg75gNUzPITZ7Zcxb0hDwIW0SsDEJJA7ni61ZlI29NS
                    /vhxUs67k6BdkzGaDs3IY9bUys54IWd4LM9CHibjnzeNN1NMuvvAacB4wnpS1pzm
                    wjznzoB9tSnphuUpZ9fwEFVJsIctvqrEUWnCQFskArGwOiaKGPVQNdCRXHnA60hS
                    jkAPAoHBANIiTesKGabUDe27phSOB2oAButYqRI6V2hGaL5QNssAIfa/Puwgm2OO
                    QsfmwTqwPIDFLOrPTRN5H9qNCGEe3UstSxB1M8F3id8Ju3OVf/KtFerap2oyFn7J
                    kTaF18DGVUqJ8PGXjYWjg28XZA0AW4016m9sqMP7qvU0DAZLJU+EZQQwtGP7dUpA
                    Opo5yOlDMNhlbKsT8xBlvTvHRzMeFYKfSAbxvByELILNyhy8P2W3eFJ6eTFtLNIo
                    4A8aD6d32QKBwQDYWly+vKut6GqLTOc+EJtdJ/eNQSzE79Lb/lOq/3BGaYDNXf10
                    JfddUbG5CWaAisnlpxNzkBQRyvnRJVev7hI9dgo0adm4u/+hOkX9GHZL/oFBBee9
                    SSRESErzlpu8p/YaaqTiRaK0ri9/hNlvRldgOgOgTbaaqEM6mERgfXI2pXEoDeVW
                    UJMftvl0t+6cnVojpRUnrL1sbGfGY4nwm3vL4BntyE4pA/J3Dt36kesmFfn6bZ45
                    EJBO1fG4b5J0VvkCgcA9zV8TUL4opdeO4xC+nOMrVuSyFTpspQIOJh8qUhc7b0eN
                    9cOA2To6dp3Hg2Ozah/xU4yZZuzpXEd1FOOxBM3bDXA1X/Mf5JOFKKycGw0th6CZ
                    mUOB5UdedNQjsAu7o9lz+cwGkidKdcPzSXLgrAlBJ+lSaifctEkcovkiZLmNgIfh
                    Sp3ThiKB+xABShuAF4XnLzVdv5lOak3UphCXxTJYX5ZkjHZiALHCqtnVx7vxo5tc
                    zq6UTeLP0LvNVUHjZEkCgcBCSsjltLL89wooN9nuM1E5sSV/5ENpuTLEQt7wSZxU
                    J04IcEHRzmL3cRWzC3e6HKvvhZvRqL4bP1CK13QK9vG9mPHyTptIZCnwfX4TBmtv
                    PQLgh+ac+mztJHn2qXMTtiDLbuW2vXHp78H4LCOvoi43ZgomI9aDHXulidA+1rP9
                    zREiBYZ0/gEpVPQerfXvxcRaGabhs/l6wiZlChFDb9xRAHtl+5RFxnxWb7HxLyLn
                    nx88bFuvk0fwM86vfveb/B8=""".replaceAll(System.lineSeparator(), "");
    private static final String ACCESS_TOKEN = "eyJraWQiOiIyNjhjY2E4OS1lMDQ2LTRjY2ItYjkxYy01YTIwMDdkNjc5NGUiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIwMGE4Y2JjYS1iNDNiLTRiZWEtYTAwOC02ZTFhOWE4MGNkOTUiLCJhdWQiOiJlZGRpZS1jb250aW51b3VzIiwibmJmIjoxNzEzMjU3MTY5LCJzY29wZSI6WyIyNF9tYWFuZGVuX2RhZ3N0YW5kZW4iXSwic2VydmljZV9pZCI6ImVkZGllLWNvbnRpbnVvdXMiLCJpc3MiOiJodHRwczovL3d3dy5hY2MubWlqbmVuZXJnaWVkYXRhLm5sL2F1dG9yaXNhdGllcmVnaXN0ZXIiLCJyZXNvdXJjZXMiOlt7ImVuZHBvaW50cyI6eyJzaW5nbGVfc3luYyI6Imh0dHBzOi8vYXBpLmFjYy5taWpuZW5lcmdpZWRhdGEubmwvZGF0YXJlZ2lzdGVycy92MS9zaW5nbGUifSwic2NvcGUiOiIyNF9tYWFuZGVuX2RhZ3N0YW5kZW4ifV0sImV4cCI6MTcxMzI1ODA2OSwiaWF0IjoxNzEzMjU3MTY5LCJqdGkiOiJmZGJhMzUxNy1jNzZiLTRhODYtYmNhMi0yM2I3NmI2MjgwNzgiLCJlYW5zIjpbIjg3MTY5MDkzMDAwMDkwOTU5NyIsIjg3MTY4NzE0MDAyNDAwMzA2NSJdLCJjb25zZW50X2lkIjoiZDAzODliY2ItMmQwNy00ZDFjLTkyOGEtNDkwOTk5Y2E2NGE5In0.YWEP4R6Oj-2sQod3hbW3pxv2mPQpqbysl1K-nKwc1Ex7U52X7YCQAv-xTX8X-dEE611IuQ2pqPf_0_dToI28eOjZSxUoAS5JUTv_wKPpNXXvGbjDV8ssRmE-6Gqcy5fFaPy3n0Medy-szUJ3x9buU98IL2cUzgJVqqBVxVOAuvJveo6WZ_9ajm91P9ID6NLAJk_W3FM8GDeKJ1hqD0GWobXcN2VBC3NeajgwsZocgihtfyX1qLKrWzn0hSmn5mZoWQWBvp4f-9c7nKdazMbc0tQBAZs5sImQDg99P8M-_r4p_MlcI7lX7wHo4uTnLSjfR6pQjZgNd5ceEhT6iMqTsw";
    private static final String ACCESS_TOKEN_RESPONSE = """
            {
                "token_type": "Bearer",
                "access_token": "%s"
            }
            """
            .formatted(ACCESS_TOKEN);
    private static final String REFRESH_AND_ACCESS_TOKEN_RESPONSE = """
            {
                "token_type": "Bearer",
                "access_token": "%s",
                "refresh_token": "asdfjkl"
            }
            """.formatted(ACCESS_TOKEN);
    private static final String ERROR_RESPONSE = "{\"error\": \"invalid_client\", \"error_description\": \"long description about the error\", \"error_uri\": \"https://error.com\"}";
    private final MijnAansluitingConfiguration config = new MijnAansluitingConfiguration(
            "",
            "",
            new ClientID("client-id"),
            new Scope("scope"),
            URI.create("http://localhost"), "jwt", URI.create("https://localhost/callback")
    );
    @Mock
    private OIDCProviderMetadata providerMetadata;
    @Mock
    private JWTProcessor<SecurityContext> jwtProcessor;
    @Mock
    private OAuthTokenRepository oAuthTokenRepository;
    @Mock
    private NlPermissionRequestRepository permissionRequestRepository;
    private OAuthManager oAuthManager;

    public static Stream<Arguments> testProcessCallback_withNegativeResponse() {
        return Stream.of(
                Arguments.of("consent_rejected", UserDeniedAuthorizationException.class),
                Arguments.of("incorrect_address", InvalidValidationAddressException.class),
                Arguments.of("access_denied", OAuthException.class)
        );
    }

    public static Stream<Arguments> testProcessCallback_withAuthorizationCode() {
        return Stream.of(
                Arguments.of(ACCESS_TOKEN_RESPONSE),
                Arguments.of(REFRESH_AND_ACCESS_TOKEN_RESPONSE)
        );
    }

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(PRIVATE_KEY);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        oAuthManager = new OAuthManager(config,
                                        privateKey,
                                        providerMetadata,
                                        jwtProcessor,
                                        oAuthTokenRepository,
                                        permissionRequestRepository);
    }

    @Test
    void testCreateAuthorizationUrl_returnsUrl() {
        // Given
        when(providerMetadata.getAuthorizationEndpointURI())
                .thenReturn(URI.create("https://localhost:8080"));

        // When
        var res = oAuthManager.createAuthorizationUrl("12");

        // Then
        assertAll(
                () -> assertNotNull(res.state()),
                () -> assertNotNull(res.codeVerifier()),
                () -> assertNotNull(res.uri())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testProcessCallback_withNegativeResponse(String errorCode, Class<Exception> exceptionClass) {
        // Given
        var callbackUri = URI.create("https://localhost/callback?error=" + errorCode);

        // When, Then
        assertThrows(exceptionClass,
                     () -> oAuthManager.processCallback(callbackUri, "pid"));
    }

    @Test
    void testProcessCallback_withUnknownPermissionRequest() {
        // Given
        var callbackUri = URI.create("https://localhost/callback?state=asdf");
        when(permissionRequestRepository.findByStateAndPermissionId("asdf", "pid"))
                .thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class,
                     () -> oAuthManager.processCallback(callbackUri, "pid"));
    }

    @ParameterizedTest
    @MethodSource
    void testProcessCallback_withAuthorizationCode(String response) throws IOException, UserDeniedAuthorizationException, JWTSignatureCreationException, OAuthUnavailableException, OAuthException, ParseException, PermissionNotFoundException, InvalidValidationAddressException, IllegalTokenException, BadJOSEException, JOSEException {
        // Given
        var server = new MockWebServer();
        var callbackUri = URI.create("https://localhost/callback?state=asdf&code=authcode");
        when(permissionRequestRepository.findByStateAndPermissionId("asdf", "pid"))
                .thenReturn(Optional.of(new MijnAansluitingPermissionRequest(
                        "pid",
                        "cid",
                        "dnid",
                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                        "asdf",
                        "rdtH6Ahq4yidFB59_0aT3HEaoDW7RjFi-viMMqN6lqc",
                        // codeChallenge = 6v8Ty3K8WRr_zbSpo4k_YkDRj-paxewCrdTlSuabLrc
                        ZonedDateTime.now(ZoneOffset.UTC),
                        null,
                        null,
                        Granularity.P1D,
                        "11", "999AB")));
        server.enqueue(
                new MockResponse()
                        .setBody(response)
                        .addHeader("Content-Type", "application/json")
        );
        server.start();
        when(providerMetadata.getTokenEndpointURI())
                .thenReturn(server.url("/callback").uri());
        when(jwtProcessor.process(isA(SignedJWT.class), any()))
                .thenReturn(new JWTClaimsSet.Builder()
                                    .issueTime(new Date())
                                    .expirationTime(new Date())
                                    .build());

        // When
        var res = oAuthManager.processCallback(callbackUri, "pid");

        // Then
        assertEquals("pid", res);

        // Clean-Up
        server.close();
    }

    @Test
    void testProcessCallback_withAuthorizationCodeAndInvalidResponse() throws IOException {
        // Given
        var server = new MockWebServer();
        var callbackUri = URI.create("https://localhost/callback?state=asdf&code=authcode");
        when(permissionRequestRepository.findByStateAndPermissionId("asdf", "pid"))
                .thenReturn(Optional.of(new MijnAansluitingPermissionRequest(
                        "pid",
                        "cid",
                        "dnid",
                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                        "asdf",
                        "rdtH6Ahq4yidFB59_0aT3HEaoDW7RjFi-viMMqN6lqc",
                        // codeChallenge = 6v8Ty3K8WRr_zbSpo4k_YkDRj-paxewCrdTlSuabLrc
                        ZonedDateTime.now(ZoneOffset.UTC),
                        null,
                        null,
                        Granularity.P1D,
                        "11", "999AB")));
        //noinspection JsonStandardCompliance
        server.enqueue(
                new MockResponse()
                        .setBody("NOT JSON")
                        .addHeader("Content-Type", "application/json")
        );
        server.start();
        when(providerMetadata.getTokenEndpointURI())
                .thenReturn(server.url("/callback").uri());

        // When

        // Then
        assertThrows(IllegalTokenException.class,
                     () -> oAuthManager.processCallback(callbackUri, "pid"));

        // Clean-Up
        server.close();
    }

    @Test
    void testProcessCallback_withAuthorizationCodeWhileServerDown() {
        // Given
        var callbackUri = URI.create("https://localhost/callback?state=asdf&code=authcode");
        when(permissionRequestRepository.findByStateAndPermissionId("asdf", "pid"))
                .thenReturn(Optional.of(new MijnAansluitingPermissionRequest(
                        "pid",
                        "cid",
                        "dnid",
                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                        "asdf",
                        "rdtH6Ahq4yidFB59_0aT3HEaoDW7RjFi-viMMqN6lqc",
                        // codeChallenge = 6v8Ty3K8WRr_zbSpo4k_YkDRj-paxewCrdTlSuabLrc
                        ZonedDateTime.now(ZoneOffset.UTC),
                        null,
                        null,
                        Granularity.P1D,
                        "11", "999AB")));
        when(providerMetadata.getTokenEndpointURI())
                .thenReturn(URI.create("https://localhost:9999/callback"));

        // When

        // Then
        assertThrows(OAuthUnavailableException.class,
                     () -> oAuthManager.processCallback(callbackUri, "pid"));
    }

    @Test
    void testProcessCallback_withAuthorizationCode_errorResponse() throws IOException {
        // Given
        var server = new MockWebServer();
        var callbackUri = URI.create("https://localhost/callback?state=asdf&code=authcode");
        when(permissionRequestRepository.findByStateAndPermissionId("asdf", "pid"))
                .thenReturn(Optional.of(new MijnAansluitingPermissionRequest(
                        "pid",
                        "cid",
                        "dnid",
                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                        "asdf",
                        "rdtH6Ahq4yidFB59_0aT3HEaoDW7RjFi-viMMqN6lqc",
                        // codeChallenge = 6v8Ty3K8WRr_zbSpo4k_YkDRj-paxewCrdTlSuabLrc
                        ZonedDateTime.now(ZoneOffset.UTC),
                        null,
                        null,
                        Granularity.P1D,
                        "11", "999AB")));
        server.enqueue(
                new MockResponse()
                        .setBody(ERROR_RESPONSE)
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(400)
        );
        server.start();
        when(providerMetadata.getTokenEndpointURI())
                .thenReturn(server.url("/callback").uri());

        // When

        // Then
        assertThrows(OAuthException.class,
                     () -> oAuthManager.processCallback(callbackUri, "pid"));

        // Clean-Up
        server.close();
    }

    @Test
    void testAccessTokenAndSingleSyncUrl_validCredentials() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, BadJOSEException, JOSEException {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pid = new OAuthTokenDetails(
                "pid",
                ACCESS_TOKEN,
                now.toInstant(),
                now.plusDays(1).toInstant(),
                null,
                null
        );
        when(oAuthTokenRepository.findById("pid"))
                .thenReturn(Optional.of(pid));
        var resources = List.of(Map.of(
                "endpoints",
                Map.of("single_sync", "https://api.acc.mijnenergiedata.nl/dataregisters/v1/single")
        ));
        when(jwtProcessor.process(isA(SignedJWT.class), any()))
                .thenReturn(new JWTClaimsSet.Builder()
                                    .claim("resources", resources)
                                    .build());

        // When
        var res = oAuthManager.accessTokenAndSingleSyncUrl("pid");

        // Then
        assertAll(
                () -> assertEquals(ACCESS_TOKEN, res.accessToken()),
                () -> assertEquals("https://api.acc.mijnenergiedata.nl/dataregisters/v1/single", res.singleSync())
        );
    }

    @Test
    void testAccessTokenAndSingleSyncUrl_invalidCredentials() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, BadJOSEException, JOSEException, IOException {
        // Given
        //noinspection resource
        var server = new MockWebServer();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var details = new OAuthTokenDetails(
                "pid",
                ACCESS_TOKEN,
                now.toInstant(),
                now.minusDays(1).toInstant(),
                "refresh_token",
                null
        );
        when(oAuthTokenRepository.findById("pid"))
                .thenReturn(Optional.of(details));
        when(oAuthTokenRepository.save(any()))
                .thenReturn(details);
        var resources = List.of(Map.of(
                "endpoints",
                Map.of("single_sync", "https://api.acc.mijnenergiedata.nl/dataregisters/v1/single")
        ));
        when(jwtProcessor.process(isA(SignedJWT.class), any()))
                .thenReturn(new JWTClaimsSet.Builder()
                                    .claim("resources", resources)
                                    .issueTime(new Date())
                                    .expirationTime(new Date())
                                    .build());
        server.enqueue(new MockResponse()
                               .addHeader("Content-Type", "application/json")
                               .setBody(ACCESS_TOKEN_RESPONSE));
        server.start();
        when(providerMetadata.getTokenEndpointURI())
                .thenReturn(server.url("/tokens").uri());
        // When
        var res = oAuthManager.accessTokenAndSingleSyncUrl("pid");

        // Then
        assertAll(
                () -> assertEquals(ACCESS_TOKEN, res.accessToken()),
                () -> assertEquals("https://api.acc.mijnenergiedata.nl/dataregisters/v1/single", res.singleSync())
        );

        // Clean-Up
        server.shutdown();
    }

    @Test
    void testAccessTokenAndSingleSyncUrl_invalidCredentials_withoutRefreshToken() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var details = new OAuthTokenDetails(
                "pid",
                ACCESS_TOKEN,
                now.toInstant(),
                now.minusDays(1).toInstant(),
                null,
                null
        );
        when(oAuthTokenRepository.findById("pid"))
                .thenReturn(Optional.of(details));
        // When, Then
        assertThrows(NoRefreshTokenException.class,
                     () -> oAuthManager.accessTokenAndSingleSyncUrl("pid"));
    }
}