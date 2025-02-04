package energy.eddie.regionconnector.cds.client;

import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class CdsPublicApisTest {
    private static MockWebServer server;
    private static String basepath;

    private final CdsPublicApis publicApis = new CdsPublicApis(WebClient.builder().build(),
                                                               new CdsConfiguration(URI.create("http://localhost")));

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        basepath = "http://localhost:" + server.getPort();
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void testCarbonSpec_returnsCarbonSpec() {
        // Given
        // language=JSON
        var body = """
                {
                  "cds_metadata_version": "v1",
                  "cds_metadata_url": "https://example.com/",
                  "created": "2025-02-04T08:38:35.846Z",
                  "updated": "2025-02-04T08:38:35.846Z",
                  "name": "string",
                  "description": "string",
                  "website": "https://example.com/",
                  "documentation": "https://example.com/",
                  "support": "https://example.com/",
                  "capabilities": [ "coverage", "oauth" ],
                  "coverage": "https://example.com/",
                  "related_metadata": [ "https://example.com/" ],
                  "commodity_types": [ "electricity" ],
                  "infrastructure_types": [ ],
                  "oauth_metadata": "https://example.com/"
                }
                """;
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(body)
                        .setResponseCode(200)
        );

        // When
        var res = publicApis.carbonDataSpec(URI.create(basepath));

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testOauthMetadataSpec_returnsOAuthMetadataSpec() {
        // Given
        // language=JSON
        var body = """
                {
                  "authorization_details_types_supported": [
                    "string"
                  ],
                  "authorization_endpoint": "https://example.com/",
                  "cds_client_settings_api": "https://example.com/",
                  "cds_client_updates_api": "https://example.com/",
                  "cds_clients_api": "https://example.com/",
                  "cds_customerdata_accounts_api": "https://example.com/",
                  "cds_customerdata_aggregations_api": "https://example.com/",
                  "cds_customerdata_billsections_api": "https://example.com/",
                  "cds_customerdata_billstatements_api": "https://example.com/",
                  "cds_customerdata_eacs_api": "https://example.com/",
                  "cds_customerdata_meterdevices_api": "https://example.com/",
                  "cds_customerdata_servicecontracts_api": "https://example.com/",
                  "cds_customerdata_servicepoints_api": "https://example.com/",
                  "cds_customerdata_usagesegments_api": "https://example.com/",
                  "cds_directory_api": "https://example.com/",
                  "cds_grants_api": "https://example.com/",
                  "cds_human_directory": "https://example.com/",
                  "cds_human_registration": "https://example.com/",
                  "cds_oauth_version": "string",
                  "cds_registration_fields": {},
                  "cds_scope_credentials_api": "https://example.com/",
                  "cds_scope_descriptions": {
                    "client_admin": {
                      "id": "client_admin",
                      "name": "client_admin",
                      "description": "string",
                      "authorization_details_fields": [],
                      "documentation": "https://example.com/",
                      "grant_types_supported": [
                        "client_credentials"
                      ],
                      "registration_optional": [],
                      "registration_requirements": [],
                      "response_types_supported": [],
                      "token_endpoint_auth_methods_supported": [
                        "string"
                      ]
                    }
                  },
                  "cds_test_accounts": "https://example.com/",
                  "code_challenge_methods_supported": [
                    "string"
                  ],
                  "grant_types_supported": [
                    "client_credentials"
                  ],
                  "introspection_endpoint": "https://example.com/",
                  "issuer": "https://example.com/",
                  "op_policy_uri": "https://example.com/",
                  "op_tos_uri": "https://example.com/",
                  "pushed_authorization_request_endpoint": "https://example.com/",
                  "registration_endpoint": "https://example.com/",
                  "response_types_supported": [
                    "string"
                  ],
                  "revocation_endpoint": "https://example.com/",
                  "scopes_supported": [
                    "client_admin"
                  ],
                  "service_documentation": "https://example.com/",
                  "token_endpoint": "https://example.com/",
                  "token_endpoint_auth_methods_supported": [
                    "string"
                  ]
                }
                """;
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(body)
                        .setResponseCode(200)
        );

        // When
        var res = publicApis.oauthMetadataSpec(URI.create(basepath));

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testCreateOAuthClient_returnsOAuthClient() {
        // Given
        // language=JSON
        var body = """
                {
                  "client_id": "string",
                  "client_id_issued_at": 0,
                  "client_name": "string",
                  "client_secret": "string",
                  "client_secret_expires_at": 0,
                  "redirect_uris": [
                    "https://example.com/"
                  ],
                  "grant_types": [
                    "string"
                  ],
                  "response_types": [
                    "string"
                  ],
                  "scope": "client_admin customer_data",
                  "token_endpoint_auth_method": "client_secret_basic",
                  "cds_server_metadata": "https://example.com/",
                  "cds_clients_api": "https://example.com/",
                  "cds_client_messages_api": "https://example.com/",
                  "cds_scope_credentials_api": "https://example.com/",
                  "cds_grants_api": "https://example.com/"
                }
                """;
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(body)
                        .setResponseCode(200)
        );

        // When
        var res = publicApis.createOAuthClient(URI.create(basepath));

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testCoverage_returnsAllPages() {
        // Given
        // language=JSON
        var coverage1 = """
                {
                  "next": "%s",
                  "previous": null,
                  "coverage_entries": [
                    {
                      "id": "string",
                      "created": "2025-02-04T09:03:23.084Z",
                      "updated": "2025-02-04T09:03:23.084Z",
                      "entity_name": "string",
                      "entity_abbreviation": "string",
                      "name": "string",
                      "description": "string",
                      "type": "geographic",
                      "role": "authoritative",
                      "infrastructure_types": [
                        "distribution_utility"
                      ],
                      "commodity_types": [
                        "electricity"
                      ],
                      "capabilities": [
                        "string"
                      ],
                      "map_resource": "https://example.com/",
                      "map_content_type": "string",
                      "geojson_resource": "https://example.com/",
                      "additionalProp1": {}
                    }
                  ]
                }
                """.formatted(basepath);
        // language=JSON
        var coverage2 = """
                {
                  "next": null,
                  "previous": null,
                  "coverage_entries": [
                    {
                      "id": "string",
                      "created": "2025-02-04T09:03:23.084Z",
                      "updated": "2025-02-04T09:03:23.084Z",
                      "entity_name": "string",
                      "entity_abbreviation": "string",
                      "name": "string",
                      "description": "string",
                      "type": "geographic",
                      "role": "authoritative",
                      "infrastructure_types": [
                        "distribution_utility"
                      ],
                      "commodity_types": [
                        "electricity"
                      ],
                      "capabilities": [
                        "string"
                      ],
                      "map_resource": "https://example.com/",
                      "map_content_type": "string",
                      "geojson_resource": "https://example.com/",
                      "additionalProp1": {}
                    }
                  ]
                }
                """;
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(coverage1)
                        .setResponseCode(200)
        );
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(coverage2)
                        .setResponseCode(200)
        );

        // When
        var res = publicApis.coverage(URI.create(basepath));

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).hasSize(2))
                    .verifyComplete();
    }
}