// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;

import static energy.eddie.regionconnector.cds.client.JsonResponses.CDS_METADATA_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;

class CdsPublicApisTest {
    private static MockWebServer server;
    private static String basepath;

    private final CdsPublicApis publicApis = new CdsPublicApis(WebClient.builder());

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
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(CDS_METADATA_RESPONSE)
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
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(JsonResponses.OAUTH_METADATA_RESPONSE)
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
                      "geojson_resource": "https://example.com/"
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