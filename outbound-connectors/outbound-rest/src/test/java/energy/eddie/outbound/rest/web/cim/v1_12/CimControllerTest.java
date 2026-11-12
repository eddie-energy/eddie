// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.cim.v1_12.vhd.MessageDocumentHeader;
import energy.eddie.cim.v1_12.vhd.MetaInformation;
import energy.eddie.outbound.rest.connectors.cim.v1_12.CimConnector;
import energy.eddie.outbound.rest.model.cim.v1_12.*;
import energy.eddie.outbound.rest.persistence.cim.v1_12.*;
import energy.eddie.outbound.rest.web.EventStream;
import energy.eddie.outbound.rest.web.WebTestConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void validatedHistoricalDataMdSSE_returnsDocuments() {
        var message1 = vhdEnvelope();
        var message2 = vhdEnvelope();

        given(cimConnector.getValidatedHistoricalDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_1_12/validated-historical-data-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(Envelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }

    @Test
    void validatedHistoricalDataMdSSEXml_returnsDocuments() {
        given(cimConnector.getValidatedHistoricalDataMarketDocumentStream())
                .willReturn(Flux.just(vhdEnvelope()));

        var result = webTestClient.get()
                                  .uri("/cim_1_12/validated-historical-data-md")
                                  .accept(EventStream.EVENT_STREAM_XML)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(String.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .assertNext(next -> assertTrue(next.contains("Envelope"), next))
                    .thenCancel()
                    .verify();
    }

    @Test
    void validatedHistoricalDataMd_returnsDocuments() {
        var msg = new ValidatedHistoricalDataMarketDocumentModel(vhdEnvelope());
        given(vhdRepository.findAll(ArgumentMatchers.<PredicateSpecification<ValidatedHistoricalDataMarketDocumentModel>>any()))
                .willReturn(List.of(msg));

        var result = webTestClient.get()
                                  .uri(uriBuilder -> uriBuilder.path("/cim_1_12/validated-historical-data-md")
                                                               .queryParam("permissionId", "pid")
                                                               .queryParam("connectionId", "cid")
                                                               .queryParam("dataNeedId", "dnid")
                                                               .queryParam("countryCode", "AT")
                                                               .queryParam("regionConnectorId", "at-eda")
                                                               .build())
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<Envelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .assertNext(next -> assertEquals(1, next.size()))
                    .verifyComplete();
    }

    @Test
    void validatedHistoricalDataMd_returnsXmlDocuments() {
        // The WebFlux test slice uses the default JAXB encoder, which does not know the CIM envelope classes.
        // The XML serialization of CIM documents is covered by FallbackXmlMessageConverterTest,
        // so this test only verifies that the endpoint serves the XML collection.
        given(vhdRepository.findAll(ArgumentMatchers.<PredicateSpecification<ValidatedHistoricalDataMarketDocumentModel>>any()))
                .willReturn(List.of());

        var result = webTestClient.get()
                                  .uri("/cim_1_12/validated-historical-data-md")
                                  .accept(MediaType.APPLICATION_XML)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(String.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .assertNext(next -> {
                        assertNotNull(next);
                        assertTrue(next.contains("ValidatedHistoricalDataMarketDocuments"), next);
                    })
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

    private static Envelope vhdEnvelope() {
        return new Envelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withMetaInformation(
                                        new MetaInformation()
                                                .withRequestPermissionId("pid")
                                                .withConnectionId("cid")
                                                .withDataNeedId("dnid")
                                                .withRegionConnector("at-eda")
                                                .withRegionCountry("AT")
                                )
                );
    }
}
