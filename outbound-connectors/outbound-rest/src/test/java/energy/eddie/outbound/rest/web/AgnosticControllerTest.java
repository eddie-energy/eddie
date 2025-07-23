package energy.eddie.outbound.rest.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
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

@WebFluxTest(AgnosticController.class)
@Import(WebTestConfig.class)
class AgnosticControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private AgnosticConnector agnosticConnector;
    @MockitoBean
    private ConnectionStatusMessageRepository repository;

    @Test
    void connectionStatusMessageSSE_returnsMessages() {
        var message1 = statusMessage(PermissionProcessStatus.CREATED);
        var message2 = statusMessage(PermissionProcessStatus.VALIDATED);

        given(agnosticConnector.getConnectionStatusMessageStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/agnostic/connection-status-messages")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(ConnectionStatusMessage.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .expectNextMatches(message -> message1.status().equals(message.status()))
                    .expectNextMatches(message -> message2.status().equals(message.status()))
                    .verifyComplete();
    }

    @Test
    void connectionStatusMessage_returnsMessages() {
        var msg = new ConnectionStatusMessageModel(statusMessage(PermissionProcessStatus.CREATED));
        given(repository.findAll(ArgumentMatchers.<Specification<ConnectionStatusMessageModel>>any()))
                .willReturn(List.of(msg));


        var result = webTestClient.get()
                                  .uri("/agnostic/connection-status-messages")
                                  .accept(MediaType.APPLICATION_JSON)
                                  .exchange()
                                  .expectStatus()
                                  .isOk()
                                  .returnResult(ConnectionStatusMessage.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    .assertNext(next -> {
                        assertNotNull(msg.payload());
                        assertEquals(msg.payload().status(), next.status());
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