package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
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
    void getContract() throws IOException {
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

    @Test
    void getContact() throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.CONTACT));
        String usagePointId = "24115050XXXXXX";

        // When & Then
        enedisApi.getContact(usagePointId)
                 .as(StepVerifier::create)
                 .assertNext(customer -> assertAll(
                         () -> assertEquals("XXXX", customer.customerId()),
                         () -> assertEquals("mailtest@gmail.com", customer.contact().email()),
                         () -> assertEquals("0512345678", customer.contact().phone())
                 ))
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {TestResourceProvider.IDENTITY, TestResourceProvider.IDENTITY_LEGAL_ONLY,
            TestResourceProvider.IDENTITY_NATURAL_ONLY})
    void getIdentity(String file) throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(file));
        String usagePointId = "24115050XXXXXX";

        // When & Then
        enedisApi.getIdentity(usagePointId)
                 .as(StepVerifier::create)
                 .assertNext(customer -> assertAll(
                         () -> assertEquals("XXXX", customer.customerId()),
                         () -> customer.identity().legalEntity().ifPresent(legalEntity -> assertAll(
                                 () -> assertEquals("SNCF Immo", legalEntity.name()),
                                 () -> assertNull(legalEntity.siretNumber()),
                                 () -> assertNull(legalEntity.business()),
                                 () -> assertNull(legalEntity.industry()),
                                 () -> assertNull(legalEntity.tradingName())
                         )),
                         () -> customer.identity().naturalPerson().ifPresent(naturalPerson -> assertAll(
                                 () -> assertEquals("Jon", naturalPerson.firstName()),
                                 () -> assertEquals("Doe", naturalPerson.lastName()),
                                 () -> assertEquals("M", naturalPerson.title())
                         ))
                 ))
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));
    }

    @Test
    void getAddress() throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.ADDRESS));
        String usagePointId = "24115050XXXXXX";

        // When & Then
        enedisApi.getAddress(usagePointId)
                 .as(StepVerifier::create)
                 .assertNext(customer -> assertAll(
                         () -> assertEquals("XXXX", customer.customerId()),
                         () -> assertEquals(1, customer.usagePoints().size()),
                         () -> assertEquals("24115050XXXXXX", customer.usagePoints().getFirst().id()),
                         () -> assertEquals("no com", customer.usagePoints().getFirst().status()),
                         () -> assertEquals("PMEI", customer.usagePoints().getFirst().meterType()),
                         () -> assertEquals("1 rue de l'homologation",
                                            customer.usagePoints().getFirst().address().street()),
                         () -> assertEquals("60000",
                                            customer.usagePoints().getFirst().address().postalCode()),
                         () -> assertEquals("BEAUVAIS",
                                            customer.usagePoints().getFirst().address().city()),
                         () -> assertEquals("France",
                                            customer.usagePoints().getFirst().address().country()),
                         () -> assertNull(customer.usagePoints().getFirst().address().geoPoints())
                 ))
                 .expectComplete()
                 .verify(Duration.ofSeconds(5));
    }
}
