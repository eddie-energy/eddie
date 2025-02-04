package energy.eddie.core.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.core.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;

@WebMvcTest(ConnectionStatusMessageController.class)
@AutoConfigureMockMvc(addFilters = false) // disables spring security filters
class ConnectionStatusMessageControllerTest {

    @MockitoBean
    private PermissionService permissionService;
    @Qualifier("webTestClient")
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void connectionStatusMessageByPermissionId_sendsStatus() {
        var message1 = statusMessage(PermissionProcessStatus.CREATED);
        var message2 = statusMessage(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        given(permissionService.getConnectionStatusMessageStream())
                .willReturn(Flux.just(message1, message2));

        var result = webTestClient.get()
                                  .uri("/api/connection-status-messages/1")
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus().isOk()
                                  .returnResult(ConnectionStatusMessage.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    // only check status to avoid time zone issues
                    .expectNextMatches(message -> message1.status().equals(message.status()))
                    .expectNextMatches(message -> message2.status().equals(message.status()))
                    .verifyComplete();
    }

    private ConnectionStatusMessage statusMessage(PermissionProcessStatus status) {
        return new ConnectionStatusMessage(
                "1",
                "1",
                "1",
                null,
                status);
    }
}
