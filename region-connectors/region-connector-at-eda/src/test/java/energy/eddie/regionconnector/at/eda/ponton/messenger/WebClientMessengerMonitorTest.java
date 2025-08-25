package energy.eddie.regionconnector.at.eda.ponton.messenger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.at.eda.ponton.messenger.WebClientMessengerMonitor.PONTON_DATE_PATTERN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebClientMessengerMonitorTest {

    static MockWebServer mockBackEnd;
    private static PontonXPAdapterConfiguration config;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModules(new Jdk8Module(), new JavaTimeModule());
    private final WebClient webClient = WebClient.create();
    @Spy
    private PontonTokenProvider tokenProvider;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        config = new PlainPontonXPAdapterConfiguration(
                "adapterId",
                "adapterVersion",
                "hostname",
                1234,
                "http://localhost:" + mockBackEnd.getPort(),
                "folder",
                "username",
                "password"
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void resendFailedMessage_sendsExpectedRequest() throws InterruptedException, JsonProcessingException {
        // Given
        var webClientMessengerMonitor = new WebClientMessengerMonitor(config, webClient, this.tokenProvider);
        when(tokenProvider.getToken()).thenReturn(Mono.just("token123"));

        // When
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        String messageId = "mId";
        webClientMessengerMonitor.resendFailedMessage(now, messageId);
        var request = mockBackEnd.takeRequest(10, TimeUnit.SECONDS);
        var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
        System.out.println(body);
        // Then
        assertAll(
                () -> assertEquals("Bearer token123", authHeader),
                () -> assertEquals(now.format(DateTimeFormatter.ofPattern(PONTON_DATE_PATTERN)),
                                   body.findValue("fromDate").asText()),
                () -> assertEquals(messageId, body.findValue("messageId").asText()),
                () -> assertEquals(config.adapterId(), body.findValue("adapterIds").elements().next().asText()),
                () -> assertEquals("FAILED", body.findValue("inboundStates").elements().next().asText())
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void resendFailedMessage_whenTokenInvalid_retriesWithNewToken() throws InterruptedException {
        // Given
        var webClientMessengerMonitor = new WebClientMessengerMonitor(config, webClient, this.tokenProvider);
        when(tokenProvider.getToken())
                .thenReturn(Mono.just("token123"))
                .thenReturn(Mono.just("token456"));

        // When
        var unauthorizedResponse = """
                {
                    "servlet": "api",
                    "message": "Unauthorized",
                    "url": "/api/messagemonitor/resendFailedMessages",
                    "status": "401"
                }
                """;
        mockBackEnd.enqueue(new MockResponse()
                                    .setBody(unauthorizedResponse)
                                    .setResponseCode(401)
                                    .addHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                                               MediaType.APPLICATION_JSON)
        );
        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(200)
                                    .addHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                                               MediaType.APPLICATION_JSON_VALUE)
        );

        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        webClientMessengerMonitor.resendFailedMessage(now, "messageId");
        VirtualTimeScheduler.getOrSet();
        var request1 = mockBackEnd.takeRequest(10, TimeUnit.SECONDS);
        var request2 = mockBackEnd.takeRequest(10, TimeUnit.SECONDS);

        // Then
        assertAll(
                () -> assertEquals("Bearer token123", request1.getHeader(HttpHeaders.AUTHORIZATION)),
                () -> assertEquals("Bearer token456", request2.getHeader(HttpHeaders.AUTHORIZATION))
        );
    }
}
