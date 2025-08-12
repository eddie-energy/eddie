package energy.eddie.outbound.rest.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.RawDataMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.RawDataMessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;

@WebFluxTest(value = AgnosticController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@Import({WebTestConfig.class, AgnosticConnector.class})
class AgnosticControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private AgnosticConnector agnosticConnector;
    @MockitoBean
    private ConnectionStatusMessageRepository csmRepository;
    @MockitoBean
    private RawDataMessageRepository rawDataRepository;

    @Test
    @DirtiesContext
    void connectionStatusMessageSSE_returnsMessages() {
        var message1 = statusMessage(PermissionProcessStatus.CREATED);
        var message2 = statusMessage(PermissionProcessStatus.VALIDATED);

        agnosticConnector.setConnectionStatusMessageStream(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/agnostic/connection-status-messages")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(ConnectionStatusMessage.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .then(agnosticConnector::close)
                    .expectNextMatches(message -> message1.status().equals(message.status()))
                    .expectNextMatches(message -> message2.status().equals(message.status()))
                    .verifyComplete();
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void connectionStatusMessage_returnsMessages() {
        var msg = new ConnectionStatusMessageModel(statusMessage(PermissionProcessStatus.CREATED));
        given(csmRepository.findAll(ArgumentMatchers.<Specification<ConnectionStatusMessageModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/agnostic/connection-status-messages")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<ConnectionStatusMessage>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .assertNext(next -> {
                        assertNotNull(next.getFirst());
                        assertEquals(msg.payload().status(), next.getFirst().status());
                    })
                    .verifyComplete();
    }

    @Test
    @DirtiesContext
    void rawDataMessageSSE_returnsMessages() {
        var message1 = new RawDataMessage("pid", "cid", "dnid", null, ZonedDateTime.now(ZoneOffset.UTC), "{}");
        var message2 = new RawDataMessage("other-pid", "cid", "dnid", null, ZonedDateTime.now(ZoneOffset.UTC), "[]");

        agnosticConnector.setRawDataStream(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/agnostic/raw-data-messages")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(RawDataMessage.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .then(agnosticConnector::close)
                    .expectNextMatches(message -> message1.permissionId().equals(message.permissionId()))
                    .expectNextMatches(message -> message2.permissionId().equals(message.permissionId()))
                    .verifyComplete();
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void rawDataMessage_returnsMessages() {
        var message = new RawDataMessage("pid", "cid", "dnid", null, ZonedDateTime.now(ZoneOffset.UTC), "{}");
        var msg = new RawDataMessageModel(message);
        given(rawDataRepository.findAll(ArgumentMatchers.<Specification<RawDataMessageModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/agnostic/raw-data-messages")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(new ParameterizedTypeReference<List<RawDataMessage>>() {})
                                  .getResponseBody();

        StepVerifier.create(result)
                    .assertNext(next -> {
                        assertNotNull(next.getFirst());
                        assertEquals(msg.payload().permissionId(), next.getFirst().permissionId());
                    })
                    .verifyComplete();
    }

    private ConnectionStatusMessage statusMessage(PermissionProcessStatus status) {
        return new ConnectionStatusMessage(
                "1",
                "1",
                "1",
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                status,
                "",
                null
        );
    }
}