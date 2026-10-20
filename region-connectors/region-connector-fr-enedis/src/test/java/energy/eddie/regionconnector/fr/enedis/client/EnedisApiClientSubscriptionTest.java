// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SuppressWarnings("DataFlowIssue")
class EnedisApiClientSubscriptionTest {
    private static final String SUBSCRIBED_SERVICES_RESPONSE = "subscribed-services-response.json";

    private static MockWebServer mockBackEnd;
    private static WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        String basePath = "http://localhost:" + mockBackEnd.getPort();
        webClient = WebClient.builder()
                             .baseUrl(basePath)
                             .build();
    }

    @Test
    void getSubscribedServices_requestsSubscribedServicesAndReturnsResponse() throws IOException, InterruptedException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(SUBSCRIBED_SERVICES_RESPONSE));

        // When
        var result = enedisApi.getSubscribedServices(999999999L);

        // Then
        StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(2L, response.totalServices());
                        assertEquals(2, response.services().size());
                        assertEquals("12345678901234", response.services().getFirst().pointId());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));

        RecordedRequest request = mockBackEnd.takeRequest();
        assertEquals("/subscribed_services/v1/", request.getPath());
        assertNotNull(request.getHeader("Authorization"));
        assertEquals("Bearer token", request.getHeader("Authorization"));
        assertNotNull(request.getBody());
        var body = request.getBody().readUtf8();
        assertTrue(body.contains("\"autorisationId\":999999999"));
        assertTrue(body.contains("\"serviceType\":\"ACCES\""));
    }
}
