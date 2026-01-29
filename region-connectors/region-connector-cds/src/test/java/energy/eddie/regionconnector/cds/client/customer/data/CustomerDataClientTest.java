package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CustomerDataClientTest {
    private static MockWebServer server;
    private static URI baseUri;
    private final JsonMapper objectMapper = new JsonMapper();
    private final ExchangeStrategies strats = ExchangeStrategies
            .builder()
            .codecs(clientDefaultCodecsConfigurer -> {
                clientDefaultCodecsConfigurer.defaultCodecs()
                                             .jacksonJsonEncoder(new JacksonJsonEncoder(objectMapper,
                                                                                        MediaType.APPLICATION_JSON));
                clientDefaultCodecsConfigurer.defaultCodecs()
                                             .jacksonJsonDecoder(new JacksonJsonDecoder(objectMapper,
                                                                                        MediaType.APPLICATION_JSON));
            }).build();
    @Spy
    @SuppressWarnings("unused")
    private final WebClient.Builder webClientBuilder = WebClient.builder()
                                                                .exchangeStrategies(strats);
    @InjectMocks
    private CustomerDataClient customerDataClient;

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUri = server.url("/").uri();
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void testUsageSegments_retrievesAllUsageSegments() {
        // Given
        // language=json
        var inner = """
                [
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
                """;
        // language=JSON
        var page1 = """
                  {
                  "next": "%s",
                  "previous": null,
                  "usage_segments": %s
                }
                """.formatted(baseUri, inner);
        // language=JSON
        var page2 = """
                  {
                  "next": null,
                  "previous": null,
                  "usage_segments": %s
                }
                """.formatted(inner);

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

        // When
        var res = customerDataClient.usageSegments(now, now, baseUri, oAuthCredentials);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }

    @Test
    void testAccounts_retrievesAllAccounts() {
        // Given
        // language=JSON
        var inner = """
                [
                  {
                    "cds_account_id": "111111111111-1",
                    "cds_created": "2024-01-01T00:00:00Z",
                    "cds_modified": "2024-01-01T00:00:00Z",
                    "cds_account_parent": null,
                    "account_number": "22222-222-22222",
                    "account_name": "Example Company",
                    "account_address": "123 Main StAnytown, TX 11111",
                    "account_type": "business",
                    "account_contacts": [
                        {
                            "type": "primary_phone",
                            "value": "+15555555555"
                        },
                        {
                            "type": "primary_email",
                            "value": "example.company@example.com"
                        }
                    ]
                  }
                ]
                """;
        // language=JSON
        var page1 = """
                {
                  "next": "%s",
                  "previous": null,
                  "accounts": %s
                }
                """.formatted(baseUri, inner);
        // language=JSON
        var page2 = """
                {
                  "next": null,
                  "previous": null,
                  "accounts": %s
                }
                """.formatted(inner);

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

        // When
        var res = customerDataClient.accounts(baseUri, oAuthCredentials);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }

    @Test
    void testServiceContracts_retrievesAllServiceContracts() {
        // Given
        // language=JSON
        var inner = """
                [
                  {
                      "cds_servicecontract_id": "3333333333-33",
                      "cds_created": "2024-01-01T00:00:00Z",
                      "cds_modified": "2024-01-01T00:00:00Z",
                      "cds_account_id": "111111111111-1",
                      "account_number": "22222-222-22222",
                      "contract_number": "333333-33-333333",
                      "contract_address": "123 Main St - PARKING LOT",
                      "contract_status": "active",
                      "contract_type": "distribution_and_supply",
                      "contract_entity": "Demo Utility",
                      "contract_start": "2024-01-01",
                      "contract_end": null,
                      "service_type": "electric",
                      "service_class": "commercial",
                      "rateplan_code": "COMM-1A",
                      "rateplan_name": "General Commerical 1A"
                  }
                ]
                """;
        // language=JSON
        var page1 = """
                {
                "next": "%s",
                "previous": null,
                "service_contracts": %s
                }
                """.formatted(baseUri, inner);
        // language=JSON
        var page2 = """
                {
                "next": null,
                "previous": null,
                "service_contracts": %s
                }
                """.formatted(inner);

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

        // When
        var res = customerDataClient.serviceContracts(baseUri, oAuthCredentials);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }

    @Test
    void testServicePoints_retrievesAllServicePoints() {
        // Given
        // language=JSON
        var inner = """
                [
                        {
                            "cds_servicepoint_id": "444444444-44",
                            "cds_created": "2024-01-01T00:00:00Z",
                            "cds_modified": "2024-01-01T00:00:00Z",
                            "servicepoint_type": "electric_meter",
                            "servicepoint_number": "P4444LOT",
                            "servicepoint_address": "123 Main St - PARKING LOT Anytown, TX 11111",
                            "latitude": null,
                            "longitude": null,
                            "current_servicecontracts": [
                                "3333333333-33"
                            ],
                            "previous_servicecontracts": [],
                            "premise_number": "P4444",
                            "premise_type": "parking_lot"
                        }
                    ]
                """;
        // language=JSON
        var page1 = """
                {
                "next": "%s",
                "previous": null,
                "service_points": %s
                }
                """.formatted(baseUri, inner);
        // language=JSON
        var page2 = """
                {
                "next": null,
                "previous": null,
                "service_points": %s
                }
                """.formatted(inner);

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

        // When
        var res = customerDataClient.servicePoints(baseUri, oAuthCredentials);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }

    @Test
    void testMeterDevices_retrievesAllMeterDevices() {
        // Given
        // language=JSON
        var inner = """
                [
                  {
                    "cds_meterdevice_id": "55555555-55",
                    "cds_created": "2024-01-01T00:00:00Z",
                    "cds_modified": "2024-01-01T00:00:00Z",
                    "meter_number": "M55555555",
                    "meter_type": "usage_forward_only",
                    "current_servicepoints": [
                        "444444444-44"
                    ],
                    "previous_servicepoints": []
                  }
                ]
                """;
        // language=JSON
        var page1 = """
                {
                "next": "%s",
                "previous": null,
                "meter_devices": %s
                }
                """.formatted(baseUri, inner);
        // language=JSON
        var page2 = """
                {
                "next": null,
                "previous": null,
                "meter_devices": %s
                }
                """.formatted(inner);

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

        // When
        var res = customerDataClient.meterDevices(baseUri, oAuthCredentials);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }
}