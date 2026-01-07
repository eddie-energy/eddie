package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Exports;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.HistoricalCollectionResponse;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.OngoingMonitoring;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GreenButtonClientTest {
    public static final Meter METER = new Meter(
            "uid",
            "1111",
            ZonedDateTime.now(ZoneOffset.UTC),
            "mail@mail.com",
            "userId",
            false,
            false,
            false,
            List.of(),
            "status",
            "",
            ZonedDateTime.now(ZoneOffset.UTC),
            new OngoingMonitoring("", null, null, null, null),
            "DEMO-UTILITY",
            0,
            List.of(),
            List.of(),
            0,
            List.of(),
            List.of(),
            new Exports(null, null, null, null, null),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
    );
    private static MockWebServer mockBackEnd;

    private static String basePath;
    private final ObjectMapper mapper = new ObjectMapperConfig().objectMapper();

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        basePath = "http://localhost:" + mockBackEnd.getPort();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }


    @Test
    void batchSubscriptionFetchesDataAndParsesIt() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var client = WebClient.create(basePath);
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/xml")
                        .setBody(XmlLoader.xmlFromResource("/xml/usagepoint/UsagePoint.xml"))
        );
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.batchSubscription("1111", "token", List.of("uid"), now, now);

        // Then
        StepVerifier.create(res)
                    .assertNext(syndFeed -> assertAll(
                            () -> assertEquals(1, syndFeed.getEntries().size()),
                            () -> assertEquals(1, syndFeed.getEntries().getFirst().getContents().size()),
                            () -> assertEquals("atom+xml",
                                               syndFeed.getEntries().getFirst().getContents().getFirst().getType())
                    ))
                    .verifyComplete();
    }

    @Test
    void batchSubscriptionEmitsException_onInvalidPayload() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var client = WebClient.create(basePath);
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/xml")
                        .setBody(XmlLoader.xmlFromResource("/xml/usagepoint/UsagePointWithInvalidPayload.xml"))
        );
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.batchSubscription("1111", "token", List.of("uid"), now, now);

        // Then
        StepVerifier.create(res)
                    .expectError()
                    .verify();
    }

    @Test
    void batchSubscription_respondsWith202_retriesAfterHeader() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var client = WebClient.create(basePath);
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(202)
                        .setHeader("Retry-After", "0")
        );
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/xml")
                        .setBody(XmlLoader.xmlFromResource("/xml/usagepoint/UsagePoint.xml"))
        );
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.batchSubscription("1111", "token", List.of("uid"), now, now);

        // Then
        StepVerifier.create(res)
                    .assertNext(syndFeed -> assertAll(
                            () -> assertEquals(1, syndFeed.getEntries().size()),
                            () -> assertEquals(1, syndFeed.getEntries().getFirst().getContents().size()),
                            () -> assertEquals("atom+xml",
                                               syndFeed.getEntries().getFirst().getContents().getFirst().getType())
                    ))
                    .verifyComplete();
    }

    @Test
    void batchSubscription_respondsWith202WithoutRetryHeader_emitsError() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var client = WebClient.create(basePath);
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(202)
        );
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.batchSubscription("1111", "token", List.of("uid"), now, now);

        // Then
        StepVerifier.create(res)
                    .expectError(NoSuchElementException.class)
                    .verify();
    }

    @Test
    void retailCustomer_fetchesDataAndParsesIt() {
        // Given
        var client = WebClient.create(basePath);
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/xml")
                        .setBody(XmlLoader.xmlFromResource("/xml/retailcustomer/accounting_point_data.xml"))
        );
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.retailCustomer("1111", "token");

        // Then
        StepVerifier.create(res)
                    .assertNext(syndFeed -> assertAll(
                            () -> assertEquals(14, syndFeed.getEntries().size()),
                            () -> assertEquals(1, syndFeed.getEntries().getFirst().getContents().size()),
                            () -> assertEquals("atom+xml",
                                               syndFeed.getEntries().getFirst().getContents().getFirst().getType())
                    ))
                    .verifyComplete();
    }

    @Test
    void collectHistoricalData_respondsWithActivatedMeteringPoints() {
        // Given
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(mapper.writeValueAsString(new HistoricalCollectionResponse(true,
                                                                                            List.of("mid1", "mid2"))))
        );

        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.collectHistoricalData(List.of("mid1", "mid2"), "company");

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertAll(
                            () -> assertTrue(resp.isSuccess()),
                            () -> assertEquals(resp.meters(), List.of("mid1", "mid2"))
                    ))
                    .verifyComplete();
    }

    @Test
    void collectHistoricalData_withEmptyMeterList_returnsMonoWithError() {
        // Given
        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.collectHistoricalData(Collections.emptyList(), "company");

        // Then
        StepVerifier.create(res)
                    .expectError(IllegalArgumentException.class)
                    .verify();
    }

    @Test
    void fetchMeters_withoutSlurp_doesNotExpandAllPages() {
        // Given
        var response = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(new MeterListing(List.of(METER), URI.create(basePath))));
        mockBackEnd.enqueue(response);
        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.fetchMeters(Pages.NO_SLURP, List.of("1111"), "company");

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void fetchMeters_withSlurp_expandsAllPages() {
        // Given
        var response1 = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(new MeterListing(List.of(METER), URI.create(basePath))));
        var response2 = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(new MeterListing(List.of(METER), null)));
        mockBackEnd.enqueue(response1);
        mockBackEnd.enqueue(response2);
        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.fetchMeters(Pages.SLURP, List.of("1111"), "company");

        // Then
        StepVerifier.create(res)
                    .expectNextCount(2)
                    .verifyComplete();
    }

    @Test
    void revoke_returnsAuthorization() throws IOException {
        // Given
        var json = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/json/authorization/authorization.json"))
                       .readAllBytes(),
                StandardCharsets.UTF_8
        );
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(json)
        );
        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.revoke("1111", "company");

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void fetchMeter_returnsMeter() {
        // Given
        var response = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(METER));
        mockBackEnd.enqueue(response);
        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client, getConfig());

        // When
        var res = api.fetchMeter("1111", "company");

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    private static GreenButtonConfiguration getConfig() {
        return new GreenButtonConfiguration(
                "http://localhost",
                Map.of(),
                Map.of(),
                Map.of("company", "token"),
                "http://localhost",
                "secret"
        );
    }
}