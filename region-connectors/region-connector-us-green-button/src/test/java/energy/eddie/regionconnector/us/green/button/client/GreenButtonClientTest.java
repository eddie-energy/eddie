package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.green.button.XmlLoader;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GreenButtonClientTest {
    private static MockWebServer mockBackEnd;

    private static String basePath;

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
        var res = api.batchSubscription("1111", "token", now, now);

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
        var res = api.batchSubscription("1111", "token", now, now);

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
        var res = api.batchSubscription("1111", "token", now, now);

        // Then
        StepVerifier.create(res)
                    .expectError(DataNotReadyException.class)
                    .verify();
    }
}