package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDataClientTest {
    private static MockWebServer server;
    private static String basepath;
    @Spy
    @SuppressWarnings("unused")
    private final WebClient webClient = WebClient.create();
    @Spy
    private final CdsServer cdsServer = new CdsServerBuilder()
            .setUsageSegmentsEndpoint(basepath)
            .setAdminClientId("client-id")
            .setAdminClientSecret("client-secret")
            .build();
    @Mock
    private CustomerDataTokenService tokenService;
    @InjectMocks
    private CustomerDataClient customerDataClient;

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        basepath = server.url("/").toString();
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void testUsageSegments_retrievesAllUsageSegments() {
        // Given
        // language=JSON
        var page1 = """
                  {
                  "next": "%s",
                  "previous": null,
                  "usage_segments": [
                    {
                      "cds_created": "2024-01-01T00:00:00Z",
                      "cds_modified": "2024-01-01T00:00:00Z",
                      "cds_usagesegment_id": "99999999-99",
                      "format": [ "kwh_net", "kwh_fwd", "kwh_rev" ],
                      "interval": 900,
                      "related_accounts": [ "111111111111-1" ],
                      "related_aggregations": [],
                      "related_billsections": [],
                      "related_meterdevices": [ "55555555-55" ],
                      "related_servicecontracts": [ "3333333333-33" ],
                      "related_servicepoints": [ "444444444-44" ],
                      "segment_end": "2024-01-01T00:00:00Z",
                      "segment_start": "2023-12-01T00:00:00Z",
                      "values": [ [ { "v": 1.2 }, { "v": 1.2 }, { "v": 0.0 } ], [ { "v": 1.1 }, { "v": 1.1 }, { "v": 0.0 } ] ]
                    }
                  ]
                }
                """.formatted(basepath);
        // language=JSON
        var page2 = """
                  {
                  "next": null,
                  "previous": null,
                  "usage_segments": [
                    {
                      "cds_created": "2024-01-01T00:00:00Z",
                      "cds_modified": "2024-01-01T00:00:00Z",
                      "cds_usagesegment_id": "99999999-99",
                      "format": [ "kwh_net", "kwh_fwd", "kwh_rev" ],
                      "interval": 900,
                      "related_accounts": [
                        "111111111111-1"
                      ],
                      "related_aggregations": [],
                      "related_billsections": [],
                      "related_meterdevices": [ "55555555-55" ],
                      "related_servicecontracts": [ "3333333333-33" ],
                      "related_servicepoints": [ "444444444-44" ],
                      "segment_end": "2024-01-01T00:00:00Z",
                      "segment_start": "2023-12-01T00:00:00Z",
                      "values": [ [ { "v": 1.2 }, { "v": 1.2 }, { "v": 0.0 } ], [ { "v": 1.1 }, { "v": 1.1 }, { "v": 0.0 } ] ]
                    }
                  ]
                }
                """;

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(page1));
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(page2));
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var oAuthCredentials = new OAuthCredentials("pid",
                                                    "refreshToken",
                                                    "accessToken",
                                                    now);
        when(tokenService.getOAuthCredentialsAsync("pid", cdsServer))
                .thenReturn(Mono.just(oAuthCredentials));
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        var res = customerDataClient.usagePoints(pr, now, now);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }
}