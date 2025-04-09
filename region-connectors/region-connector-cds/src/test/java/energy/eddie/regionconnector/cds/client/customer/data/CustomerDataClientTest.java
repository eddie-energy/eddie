package energy.eddie.regionconnector.cds.client.customer.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.cds.CdsBeanConfig;
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
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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
    private final ObjectMapper objectMapper = new CdsBeanConfig().customObjectMapper().build();
    private final ExchangeStrategies strats = ExchangeStrategies
            .builder()
            .codecs(clientDefaultCodecsConfigurer -> {
                clientDefaultCodecsConfigurer.defaultCodecs()
                                             .jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper,
                                                                                          MediaType.APPLICATION_JSON));
                clientDefaultCodecsConfigurer.defaultCodecs()
                                             .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper,
                                                                                          MediaType.APPLICATION_JSON));
            }).build();
    @Spy
    @SuppressWarnings("unused")
    private final WebClient webClient = WebClient.builder()
                                                 .exchangeStrategies(strats)
                                                 .build();
    @Spy
    private final CdsServer cdsServer = new CdsServerBuilder()
            .setAccountsEndpoint(basepath)
            .setServiceContractsEndpoint(basepath)
            .setServicePointsEndpoint(basepath)
            .setMeterDeviceEndpoint(basepath)
            .setUsageSegmentsEndpoint(basepath)
            .setBillSectionEndpoint(basepath)
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
                """.formatted(basepath, inner);
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
                """.formatted(basepath, inner);
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
        when(tokenService.getOAuthCredentialsAsync("pid", cdsServer))
                .thenReturn(Mono.just(oAuthCredentials));
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        var res = customerDataClient.accounts(pr);

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
                """.formatted(basepath, inner);
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
        when(tokenService.getOAuthCredentialsAsync("pid", cdsServer))
                .thenReturn(Mono.just(oAuthCredentials));
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        var res = customerDataClient.serviceContracts(pr);

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
                """.formatted(basepath, inner);
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
        when(tokenService.getOAuthCredentialsAsync("pid", cdsServer))
                .thenReturn(Mono.just(oAuthCredentials));
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        var res = customerDataClient.servicePoints(pr);

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
                """.formatted(basepath, inner);
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
        when(tokenService.getOAuthCredentialsAsync("pid", cdsServer))
                .thenReturn(Mono.just(oAuthCredentials));
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        var res = customerDataClient.meterDevices(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }

    @Test
    void testBillSections_retrievesAllBillSections() {
        // Given
        // language=JSON
        var inner = """
                [
                {
                            "cds_billsection_id": "77777777-77",
                            "cds_created": "2024-01-01T00:00:00Z",
                            "cds_modified": "2024-01-01T00:00:00Z",
                            "cds_billstatement_id": "66666666-66",
                            "cds_account_id": "111111111111-1",
                            "account_number": "22222-222-22222",
                            "section_type": "distribution_and_supply_charges",
                            "start_date": "2023-12-01",
                            "end_date": "2023-12-31",
                            "currency": "USD",
                            "distribution_entity": {
                                "type": "distributor",
                                "entity": "Demo Utility",
                                "cds_servicecontract_id": "3333333333-33",
                                "contract_number": "333333-33-333333",
                                "rateplan_code": "COMM-1A",
                                "rateplan_name": "General Commerical 1A",
                                "program_participations": [
                                    {
                                        "program_type": "aggregated_demand_response",
                                        "program_id": "adr_app01",
                                        "name": "Acme Demand Response App"
                                    }
                                ]
                            },
                            "load_serving_entity": {
                                "type": "distributor"
                            },
                
                            "related_servicecontracts": [
                                "3333333333-33"
                            ],
                            "related_servicepoints": [
                                "444444444-44"
                            ],
                            "related_meterdevices": [
                                "55555555-55"
                            ],
                            "related_billsections": [],
                
                            "line_items": [
                                {
                                    "item_type": "charge",
                                    "for": "distribution_entity",
                                    "name": "Electric delivery",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "cost": 2.00,
                                    "value": 50.0,
                                    "rate": 0.0400,
                                    "unit": "kwh"
                                },
                                {
                                    "item_type": "subtotal",
                                    "for": "distribution_entity",
                                    "name": "Total delivery charges",
                                    "cost": 2.00
                                },
                                {
                                    "item_type": "charge",
                                    "for": "load_serving_entity",
                                    "name": "Electric supply",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "cost": 2.00,
                                    "value": 50.0,
                                    "rate": 0.0400,
                                    "unit": "kwh"
                                },
                                {
                                    "item_type": "subtotal",
                                    "for": "load_serving_entity",
                                    "name": "Total supply charges",
                                    "cost": 2.00
                                },
                                {
                                    "item_type": "charge",
                                    "for": "other",
                                    "name": "Local taxes",
                                    "cost": 0.99
                                },
                                {
                                    "item_type": "subtotal",
                                    "for": "other",
                                    "name": "Total other charges",
                                    "cost": 0.99
                                },
                                {
                                    "item_type": "total",
                                    "for": null,
                                    "name": "Total electric charges",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "cost": 2.00
                                }
                            ],
                
                            "energy_summary": [
                                {
                                    "summary_type": "meter_reading",
                                    "cds_meterdevice_id": "55555555-55",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "start_reading": 100000,
                                    "end_reading": 100050,
                                    "multiplier": 1,
                                    "adjustment": 0,
                                    "value": 50.0,
                                    "unit": "kwh"
                                },
                                {
                                    "summary_type": "usage_net",
                                    "name": "Net Consumption",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "value": 50.0,
                                    "unit": "kwh",
                                    "tier_level": null,
                                    "tou_bucket": null
                                },
                                {
                                    "summary_type": "usage_reverse",
                                    "name": "Feed-in Generation",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "value": -1.0,
                                    "unit": "kwh",
                                    "tier_level": null,
                                    "tou_bucket": null
                                },
                                {
                                    "summary_type": "usage_forward",
                                    "name": "Total Consumption",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "value": 51.0,
                                    "unit": "kwh",
                                    "tier_level": null,
                                    "tou_bucket": null
                                },
                                {
                                    "summary_type": "demand",
                                    "name": "Commercial Peak Demand",
                                    "start": "2023-12-01T00:00:00Z",
                                    "end": "2024-01-01T00:00:00Z",
                                    "value": 0.0,
                                    "unit": "kw",
                                    "tier_level": null,
                                    "tou_bucket": null
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
                "bill_sections": %s
                }
                """.formatted(basepath, inner);
        // language=JSON
        var page2 = """
                {
                "next": null,
                "previous": null,
                "bill_sections": %s
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
        when(tokenService.getOAuthCredentialsAsync("pid", cdsServer))
                .thenReturn(Mono.just(oAuthCredentials));
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        var res = customerDataClient.billSections(pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertThat(list).hasSize(2))
                    .verifyComplete();
    }
}