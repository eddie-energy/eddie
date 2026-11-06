// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v1_12.CimConnector;
import energy.eddie.outbound.rest.model.cim.v1_12.*;
import energy.eddie.outbound.rest.persistence.cim.v1_12.*;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@WebFluxTest(value = CimController.class, excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class)
@Import({WebTestConfig.class})
class CimControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CimConnector cimConnector;
    @MockitoBean
    private NearRealTimeDataMarketDocumentRepository rtdRepository;
    @MockitoBean
    private AcknowledgementMarketDocumentRepository ackRepository;
    @MockitoBean
    private EnergySharingReferenceDataMarketDocumentRepository esrRepository;
    @MockitoBean
    private MinMaxEnvelopeMarketDocumentRepository minMaxRepository;
    @MockitoBean
    private RequestPermissionMarketDocumentRepository rpmdRepository;

    @MockitoBean
    private ValidatedHistoricalDataMarketDocumentRepository vhdRepository;

    @Test
    void nearRealTimeDataMdSSE_returnsDocuments() {
        var message1 = new RTDEnvelope();
        var message2 = new RTDEnvelope();

        given(cimConnector.getNearRealTimeDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_12/near-real-time-data-md")
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
                                  .uri("/cim_1_12/near-real-time-data-md")
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

    @Test
    void acknowledgementMdSSE_returnsDocuments() {
        var message1 = new AcknowledgementEnvelope();
        var message2 = new AcknowledgementEnvelope();

        given(cimConnector.getAcknowledgementMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_12/acknowledgement-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(AcknowledgementEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void acknowledgementMd_returnsDocuments() {
        var msg = new AcknowledgementMarketDocumentModel(new AcknowledgementEnvelope());
        given(ackRepository.findAll(ArgumentMatchers.<Specification<AcknowledgementMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_1_12/acknowledgement-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<AcknowledgementEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void minMaxEnvelopeMd_returnsAccepted() {
        var msg = new RECMMOEEnvelope();

        webTestClient.post()
                     .uri("/cim_1_12/min-max-envelope-md")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(msg)
                     .exchange()
                     .expectStatus()
                     .isAccepted();
    }

    @Test
    void minMaxEnvelopeMdSSE_returnsDocuments() {
        var message1 = new RECMMOEEnvelope();
        var message2 = new RECMMOEEnvelope();

        given(cimConnector.getForwardedMinMaxEnvelopeStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                 .uri("/cim_1_12/min-max-envelope-md")
                                 .accept(MediaType.TEXT_EVENT_STREAM)
                                 .exchange()
                                 .expectStatus()
                                 .isOk()
                                 .returnResult(RECMMOEEnvelope.class)
                                 .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }

    @Test
    void minMaxEnvelopeMd_returnsDocuments() {
        var msg = new MinMaxEnvelopeMarketDocumentModel(new RECMMOEEnvelope());
        given(minMaxRepository.findAll(ArgumentMatchers.<Specification<MinMaxEnvelopeMarketDocumentModel>>any()))
                .willReturn(List.of(msg));

        var result = webTestClient.get()
                                 .uri("/cim_1_12/min-max-envelope-md")
                                 .accept(MediaType.APPLICATION_JSON)
                                 .exchange()
                                 .expectStatus()
                                 .isOk()
                                 .returnResult(new ParameterizedTypeReference<List<RECMMOEEnvelope>>() {})
                                 .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void minMaxEnvelopeMd_returnsXmlDocuments() {
        var msg = new MinMaxEnvelopeMarketDocumentModel(new RECMMOEEnvelope());
        given(minMaxRepository.findAll(ArgumentMatchers.<Specification<MinMaxEnvelopeMarketDocumentModel>>any()))
                .willReturn(List.of(msg));

        var result = webTestClient.get()
                                 .uri("/cim_1_12/min-max-envelope-md")
                                 .accept(MediaType.APPLICATION_XML)
                                 .exchange()
                                 .expectStatus()
                                 .isOk()
                                 .returnResult(String.class)
                                 .getResponseBody();

        StepVerifier.create(result)
                    .assertNext(next -> {
                        assertNotNull(next);
                        assertTrue(next.contains("MinMaxEnvelopeMarketDocuments"));
                    })
                    .verifyComplete();
    }


    @Test
    void energySharingReferenceDataMdSSE_returnsDocuments() {
        var message1 = new ESRDMDEnvelope();
        var message2 = new ESRDMDEnvelope();

        given(cimConnector.getEnergySharingReferenceDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_12/energy-sharing-reference-data-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(ESRDMDEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void energySharingReferenceDataMd_returnsDocuments() {
        var msg = new EnergySharingReferenceDataMarketDocumentModel(new ESRDMDEnvelope());
        given(esrRepository.findAll(ArgumentMatchers.<Specification<EnergySharingReferenceDataMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_1_12/energy-sharing-reference-data-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<ESRDMDEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void requestPermissionMdSSE_returnsDocuments() {
        var message1 = new RequestPermissionEnvelope();
        var message2 = new RequestPermissionEnvelope();

        given(cimConnector.getRequestPermissionMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_12/request-permission-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(RequestPermissionEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void requestPermissionMd_returnsDocuments() {
        var msg = new RequestPermissionMarketDocumentModel(new RequestPermissionEnvelope());
        given(rpmdRepository.findAll(ArgumentMatchers.<Specification<RequestPermissionMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_1_12/request-permission-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<RequestPermissionEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }
    @Test
    void validatedHistoricalDataMdSSE_returnsDocuments() {
        var message1 = new energy.eddie.cim.v1_12.vhd.Envelope();
        var message2 = new energy.eddie.cim.v1_12.vhd.Envelope();

        given(cimConnector.getValidatedHistoricalDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_12/validated-historical-data-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(energy.eddie.cim.v1_12.vhd.Envelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void validatedHistoricalDataMd_returnsDocuments() {
        var msg = new ValidatedHistoricalDataMarketDocumentModel(new energy.eddie.cim.v1_12.vhd.Envelope());
        given(vhdRepository.findAll(ArgumentMatchers.<Specification<ValidatedHistoricalDataMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_1_12/validated-historical-data-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<energy.eddie.cim.v1_12.vhd.Envelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void validatedHistoricalDataLiveXmlPreservesNestedPermission() {
        var message = historicalMessage();
        given(cimConnector.getValidatedHistoricalDataMarketDocumentStream()).willReturn(Flux.just(message));
        var result = webTestClient.get().uri("/cim_1_12/validated-historical-data-md")
                .accept(MediaType.parseMediaType(energy.eddie.outbound.rest.web.EventStream.EVENT_STREAM_XML_VALUE))
                .exchange().expectStatus().isOk().returnResult(String.class).getResponseBody();
        StepVerifier.create(result).assertNext(xml -> {
            assertTrue(xml.contains("requestPermissionId"));
            assertTrue(xml.contains("pid"));
            assertTrue(xml.contains("CEEDS_ValidatedHistoricalDataDocument_v1.12.38"));
        }).verifyComplete();
    }

    @Test
    void validatedHistoricalDataStoredJsonPreservesNestedPermission() {
        given(vhdRepository.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.PredicateSpecification<ValidatedHistoricalDataMarketDocumentModel>>any()))
                .willReturn(List.of(new ValidatedHistoricalDataMarketDocumentModel(historicalMessage())));
        webTestClient.get().uri("/cim_1_12/validated-historical-data-md?permissionId=pid")
                .accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
                .expectBody(String.class).value(json -> {
                    assertTrue(json.contains("requestPermissionId"));
                    assertTrue(json.contains("pid"));
                });
    }

    private static energy.eddie.cim.v1_12.vhd.Envelope historicalMessage() {
        return new energy.eddie.cim.v1_12.vhd.Envelope().withMessageDocumentHeader(
                new energy.eddie.cim.v1_12.vhd.MessageDocumentHeader().withMetaInformation(
                        new energy.eddie.cim.v1_12.vhd.MetaInformation().withRequestPermissionId("pid")
                                .withConnectionId("cid").withDataNeedId("dnid").withRegionCountry("AT").withRegionConnector("at-eda")));
    }
}