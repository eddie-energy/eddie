package energy.eddie.outbound.rest.web.cim.v0_82;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.model.cim.v0_82.AccountingPointDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.PermissionMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v0_82.AccountingPointDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.PermissionMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
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

@WebFluxTest(value = CimController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@Import({WebTestConfig.class})
class CimControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CimConnector cimConnector;
    @MockitoBean
    private ValidatedHistoricalDataMarketDocumentRepository vhdRepository;
    @MockitoBean
    private PermissionMarketDocumentRepository pmdRepository;
    @MockitoBean
    private AccountingPointDataMarketDocumentRepository apRepository;

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
        given(vhdRepository.findAll(ArgumentMatchers.<Specification<ValidatedHistoricalDataMarketDocumentModel>>any()))
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

    @Test
    void permissionMdSSE_returnsDocuments() {
        var message1 = new PermissionEnvelope();
        var message2 = new PermissionEnvelope();

        given(cimConnector.getPermissionMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_0_82/permission-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(PermissionEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void permissionMd_returnsDocuments() {
        var msg = new PermissionMarketDocumentModel(new PermissionEnvelope());
        given(pmdRepository.findAll(ArgumentMatchers.<Specification<PermissionMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_0_82/permission-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<PermissionEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void accountingPointDataMdSSE_returnsDocuments() {
        var message1 = new AccountingPointEnvelope();
        var message2 = new AccountingPointEnvelope();

        given(cimConnector.getAccountingPointDataMarketDocumentStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/cim_0_82/accounting-point-data-md")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(AccountingPointEnvelope.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
    }


    @Test
    void accountingPointDataMd_returnsDocuments() {
        var msg = new AccountingPointDataMarketDocumentModel(new AccountingPointEnvelope());
        given(apRepository.findAll(ArgumentMatchers.<Specification<AccountingPointDataMarketDocumentModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/cim_0_82/accounting-point-data-md")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<AccountingPointEnvelope>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}