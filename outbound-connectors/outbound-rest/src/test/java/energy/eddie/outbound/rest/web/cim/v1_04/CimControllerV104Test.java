// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v1_04.CimConnectorV1_04;
import energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v1_04.ValidatedHistoricalDataMarketDocumentModelV1_04;
import energy.eddie.outbound.rest.persistence.cim.v1_04.NearRealTImeDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v1_04.ValidatedHistoricalDataMarketDocumentV1_04Repository;
import energy.eddie.outbound.rest.web.WebTestConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
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

@WebFluxTest(value = CimControllerV1_04.class, excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class)
@Import({WebTestConfig.class})
class CimControllerV104Test {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CimConnectorV1_04 cimConnector;
    @MockitoBean
    private ValidatedHistoricalDataMarketDocumentV1_04Repository vhdRepository;
    @MockitoBean
    private NearRealTImeDataMarketDocumentRepository rtdRepository;

    @Test
    void validatedHistoricalDataMdSSE_returnsDocuments() {
        var message1 = new VHDEnvelope();
        var message2 = new VHDEnvelope();

        given(cimConnector.getValidatedHistoricalDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_04/validated-historical-data-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(VHDEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void validatedHistoricalDataMd_returnsDocuments() {
        var msg = new ValidatedHistoricalDataMarketDocumentModelV1_04(new VHDEnvelope());
        given(vhdRepository.findAll(ArgumentMatchers.<Specification<ValidatedHistoricalDataMarketDocumentModelV1_04>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_1_04/validated-historical-data-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<VHDEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }

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