package energy.eddie.regionconnector.at.eda.ponton.messenger.client;

import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messenger.PontonTokenProvider;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.FindMessages;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.MessageCategory;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.Status;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindMessagesClientTest {
    private static final MockWebServer SERVER = new MockWebServer();
    private final PontonXPAdapterConfiguration config = new PlainPontonXPAdapterConfiguration(
            "adapter-id",
            "1.0",
            SERVER.url("/").toString(),
            SERVER.getPort(),
            SERVER.url("/api").toString(),
            "/",
            "admin",
            "admin"
    );
    @Mock
    private PontonTokenProvider tokenProvider;

    @BeforeAll
    static void beforeAll() throws IOException {
        SERVER.start();
    }

    @AfterAll
    static void afterAll() throws IOException {
        SERVER.shutdown();
    }

    @Test
    void findMessages_findsMessages() {
        // Given
        var creationTime = ZonedDateTime.parse("2025-08-13T10:10:46.266Z");
        // language=JSON
        var json = """
                {
                "totalResultCount": 1,
                "messages": [
                    {
                      "id": 1,
                      "inbound":true,
                      "status":"OK",
                      "test":false,
                      "creationTime":"2025-08-13T10:10:46.266Z",
                      "registrationTime":"2025-08-13T10:10:46.756Z",
                      "senderId":"at000000",
                      "receiverId":"000000000000000000000000000000",
                      "messageId":"AT000000000000000000000000000000000@wn.wienit.at",
                      "conversationId":"EP00000000000000000000",
                      "sequenceNumber":null,
                      "schemaSet":"CM_REQ_ONL_01.30",
                      "messageType":"ANTWORT_CCMO",
                      "transmissionProtocol":"HTTPS",
                      "packager":"AS4",
                      "adapterId":"adapter-id",
                      "ackReceived":false,
                      "logInfo":"Set:CM_REQ_ONL_01.30 IN:1 IB:/IDXAT2/CCM_NOTIFICATION2 IM:/IDXAT2/CCM_NOTIFICATION2 R:EP000000 SPOR:WN3CLNT100",
                      "clusterNodeId":1
                    }
                  ]
                }
                """;
        when(tokenProvider.getToken()).thenReturn(Mono.just("token"));
        var client = new FindMessagesClient(WebClient.create(), tokenProvider, config);
        SERVER.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(json));

        // When
        var res = client.findMessages(new FindMessages("adapter-id",
                                                       Status.OK,
                                                       creationTime,
                                                       MessageCategory.PRODUCTION));

        // Then
        StepVerifier.create(res)
                    .assertNext(message ->
                                        assertEquals(1, message.totalResultCount())
                    )
                    .verifyComplete();
    }
}