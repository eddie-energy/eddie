package energy.eddie.regionconnector.fr.enedis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.fr.enedis.FrEnedisSpringConfig;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class EnedisApiClientMasterDataTest {
    private static MockWebServer mockBackEnd;
    private static WebClient webClient;
    private final ObjectMapper objectMapper = new FrEnedisSpringConfig().objectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        String basePath = "http://localhost:" + mockBackEnd.getPort();
        webClient = WebClient.builder()
                             .baseUrl(basePath)
                             .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
                                                                                   .jackson2JsonDecoder(new Jackson2JsonDecoder(
                                                                                           objectMapper,
                                                                                           MediaType.APPLICATION_JSON)))
                             .build();
    }

    @Test
    void getContract_returnsContract() throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.CONTRACT));
        String usagePointId = "24115050XXXXXX";

        // When & Then
        enedisApi.getContract(usagePointId)
                 .as(StepVerifier::create)
                 .assertNext(customer -> assertAll(
                         () -> assertEquals("XXXX", customer.customerId()),
                         () -> assertEquals(1, customer.usagePointContracts().size()),
                         () -> assertEquals("24115050XXXXXX",
                                            customer.usagePointContracts().getFirst().usagePoint().id()),
                         () -> assertEquals("com", customer.usagePointContracts().getFirst().usagePoint().status()),
                         () -> assertEquals("AMM", customer.usagePointContracts().getFirst().usagePoint().meterType()),
                         () -> assertEquals("C5", customer.usagePointContracts().getFirst().contract().segment()),
                         () -> assertEquals("6 kVA",
                                            customer.usagePointContracts().getFirst().contract().subscribedPower()),
                         () -> assertEquals("2017-07-15+02:00",
                                            customer.usagePointContracts().getFirst().contract().lastActivationDate()),
                         () -> assertEquals("BTINFMU4",
                                            customer.usagePointContracts().getFirst().contract().distributionTariff()),
                         () -> assertEquals("HC (22H50-6H50)",
                                            customer.usagePointContracts().getFirst().contract().offPeakHours()),
                         () -> assertEquals("Contrat GRD-F",
                                            customer.usagePointContracts().getFirst().contract().contractType()),
                         () -> assertEquals("SERVC",
                                            customer.usagePointContracts().getFirst().contract().contractStatus()),
                         () -> assertEquals("2024-05-11+02:00",
                                            customer.usagePointContracts()
                                                    .getFirst()
                                                    .contract()
                                                    .lastDistributionTariffChangeDate())
                 ))
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));
    }
}
