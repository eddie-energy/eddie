// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class EnedisApiClientMasterDataTest {
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
    void getContract_callsSituationContractuelleEndpoint() throws IOException, InterruptedException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.SITUATION_CONTRACTUELLE));

        // When & Then
        enedisApi.getContract("3127069600")
                 .as(StepVerifier::create)
                 .assertNext(situations -> {
                     assertEquals(1, situations.size());
                     var situation = situations.getFirst();
                     assertEquals("3127069600", situation.usagePointId());
                     assertEquals(List.of("C5"), situation.segments());
                 })
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));

        RecordedRequest request = mockBackEnd.takeRequest();
        assertEquals("/situation_contrat_auto/v1/3127069600", request.getPath());
    }

    @Test
    void getContract_withObjectPayload_stillReturnsSingleSituation() {
        // Given: tolerate a payload that is not wrapped in an array
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(new MockResponse()
                                    .setBody("""
                                                     {"usage_point_id": "3127069600", "segment": "C5"}
                                                     """)
                                    .addHeader("Content-Type", "application/json"));

        // When & Then
        enedisApi.getContract("3127069600")
                 .as(StepVerifier::create)
                 .assertNext(situations -> {
                     assertEquals(1, situations.size());
                     assertEquals("3127069600", situations.getFirst().usagePointId());
                 })
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void getAddress_callsDonneesGeneralesEndpoint() throws IOException, InterruptedException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.GENERAL_DATA));

        // When & Then
        enedisApi.getAddress("24115050XXXXXX")
                 .as(StepVerifier::create)
                 .assertNext(data -> {
                     assertEquals("75112", data.address().address().inseeCode());
                     assertEquals("75000 Paris", data.address().address().postalCodeCity());
                 })
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));

        RecordedRequest request = mockBackEnd.takeRequest();
        assertEquals("/donnees_generales_auto/v1/24115050XXXXXX", request.getPath());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void getAddress_withArrayPayload_stillReturnsAddress() {
        // Given: tolerate an array payload, since the sibling API returns one despite its docs
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(new MockResponse()
                                    .setBody("""
                                                     [{"address": {"address": {"insee_code": "75112", "postal_code_city": "75000"}}}]
                                                     """)
                                    .addHeader("Content-Type", "application/json"));

        // When & Then
        enedisApi.getAddress("3127069600")
                 .as(StepVerifier::create)
                 .assertNext(data -> {
                     assertEquals("75112", data.address().address().inseeCode());
                     assertEquals("75000", data.address().address().postalCodeCity());
                 })
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));
    }

    @Test
    void getAddress_withEmptyPayload_emitsEmptyAddressRatherThanCompletingEmpty() {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(new MockResponse()
                                    .setBody("[]")
                                    .addHeader("Content-Type", "application/json"));

        // When & Then: an empty Mono here would make the zip in AccountingPointDataService never emit
        enedisApi.getAddress("3127069600")
                 .as(StepVerifier::create)
                 .assertNext(data -> assertNull(data.address()))
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));
    }
}
