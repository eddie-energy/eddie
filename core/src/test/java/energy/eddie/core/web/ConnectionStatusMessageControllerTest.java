// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.core.security.JwtIssuerFilter;
import energy.eddie.core.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;

@WebFluxTest(
        value = ConnectionStatusMessageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtIssuerFilter.class)
)
@AutoConfigureMockMvc(addFilters = false) // disables spring security filters
class ConnectionStatusMessageControllerTest {
    @MockitoBean
    private PermissionService permissionService;
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void connectionStatusMessageByPermissionId_sendsStatus() {
        var message1 = statusMessage("1", PermissionProcessStatus.CREATED);
        var message2 = statusMessage("1", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

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

    @Test
    void connectionStatusMessageByPermissionIds_sendsStatus() {
        var message1 = statusMessage("1", PermissionProcessStatus.CREATED);
        var message2 = statusMessage("2", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        var message3 = statusMessage("3", PermissionProcessStatus.VALIDATED);

        given(permissionService.getConnectionStatusMessageStream())
                .willReturn(Flux.just(message1, message2, message3));

        var result = webTestClient.get()
                                  .uri(b -> b.path("/api/connection-status-messages")
                                             .queryParam("permission-id", "1", "3")
                                             .build())
                                  .accept(MediaType.TEXT_EVENT_STREAM)
                                  .exchange()
                                  .expectStatus().isOk()
                                  .returnResult(ConnectionStatusMessage.class)
                                  .getResponseBody();

        StepVerifier.create(result)
                    // only check status to avoid time zone issues
                    .expectNextMatches(message -> message1.status().equals(message.status()))
                    .expectNextMatches(message -> message3.status().equals(message.status()))
                    .verifyComplete();
    }

    private ConnectionStatusMessage statusMessage(String pid, PermissionProcessStatus status) {
        return new ConnectionStatusMessage(
                "1",
                pid,
                "1",
                null,
                status);
    }
}
