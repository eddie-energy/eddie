package energy.eddie.regionconnector.us.green.button.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.*;
import energy.eddie.regionconnector.us.green.button.exceptions.DataNotReadyException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

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
        var api = new GreenButtonClient(client);

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
        var api = new GreenButtonClient(client);

        // When
        var res = api.batchSubscription("1111", "token", List.of("uid"), now, now);

        // Then
        StepVerifier.create(res)
                    .expectError()
                    .verify();
    }


    @Test
    void batchSubscription_respondsWith202_causesException() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var client = WebClient.create(basePath);
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(202)
        );
        var api = new GreenButtonClient(client);

        // When
        var res = api.batchSubscription("1111", "token", List.of("uid"), now, now);

        // Then
        StepVerifier.create(res)
                    .expectError(DataNotReadyException.class)
                    .verify();
    }

    @Test
    void collectHistoricalDAta_respondsWithActivatedMeteringPoints() throws JsonProcessingException {
        // Given
        mockBackEnd.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(mapper.writeValueAsString(new HistoricalCollectionResponse(true,
                                                                                            List.of("mid1", "mid2"))))
        );

        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client);

        // When
        var res = api.collectHistoricalData(List.of("mid1", "mid2"));

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertAll(
                            () -> assertTrue(resp.isSuccess()),
                            () -> assertEquals(resp.meters(), List.of("mid1", "mid2"))
                    ))
                    .verifyComplete();
    }

    @Test
    void fetchInactiveMeters_withoutSlurp_doesNotExpandAllPages() throws JsonProcessingException {
        // Given
        var response = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(new MeterListing(List.of(METER), URI.create(basePath))));
        mockBackEnd.enqueue(response);
        var client = WebClient.create(basePath);
        var api = new GreenButtonClient(client);

        // When
        var res = api.fetchInactiveMeters(Pages.NO_SLURP, List.of("1111"));

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void fetchInactiveMeters_withSlurp_expandsAllPages() throws JsonProcessingException {
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
        var api = new GreenButtonClient(client);

        // When
        var res = api.fetchInactiveMeters(Pages.SLURP, List.of("1111"));

        // Then
        StepVerifier.create(res)
                    .expectNextCount(2)
                    .verifyComplete();
    }
}