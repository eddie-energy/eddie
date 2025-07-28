package energy.eddie.outbound.rest.web.cim.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import energy.eddie.outbound.rest.web.WebTestConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebFluxTest(CimController.class)
@Import({WebTestConfig.class})
class CimControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CimConnector cimConnector;
    @MockitoBean
    private ValidatedHistoricalDataMarketDocumentRepository repository;

    @Test
    void validatedHistoricalDataMdSSE_returnsDocuments() {
        var message1 = new ValidatedHistoricalDataEnvelope();
        var message2 = new ValidatedHistoricalDataEnvelope();

        given(cimConnector.getHistoricalDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_0_82/validated-historical-data-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(ValidatedHistoricalDataEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void validatedHistoricalDataMd_returnsDocuments() {
        var msg = new ValidatedHistoricalDataMarketDocumentModel(new ValidatedHistoricalDataEnvelope());
        given(repository.findAll(ArgumentMatchers.<Specification<ValidatedHistoricalDataMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_0_82/validated-historical-data-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<ValidatedHistoricalDataEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}