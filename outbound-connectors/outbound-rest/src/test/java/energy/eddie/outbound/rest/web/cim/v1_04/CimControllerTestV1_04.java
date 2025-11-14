package energy.eddie.outbound.rest.web.cim.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v1_04.CimConnectorV1_04;
import energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v1_04.NearRealTImeDataMarketDocumentRepository;
import energy.eddie.outbound.rest.web.WebTestConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.BDDMockito.given;

@WebFluxTest(value = CimControllerV1_04.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@Import({WebTestConfig.class})
@SuppressWarnings("java:S101")
class CimControllerTestV1_04 {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CimConnectorV1_04 cimConnector;
    @MockitoBean
    private NearRealTImeDataMarketDocumentRepository rtdRepository;

    @Test
    void nearRealTimeDataMdSSE_returnsDocuments() {
        var message1 = new RTDEnvelope();
        var message2 = new RTDEnvelope();

        given(cimConnector.getNearRealTimeDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_04/near-real-time-data-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(RTDEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void nearRealTimeDataMd_returnsDocuments() {
        var msg = new NearRealTimeDataMarketDocumentModel(new RTDEnvelope());
        given(rtdRepository.findAll(ArgumentMatchers.<Specification<NearRealTimeDataMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_1_04/near-real-time-data-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<RTDEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}