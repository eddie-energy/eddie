// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.web;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.core.security.JwtIssuerFilter;
import energy.eddie.core.services.MessageStreamHub;
import energy.eddie.spring.regionconnector.extensions.StreamProviderAndSupplierRegistrar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

@WebFluxTest(
        value = ConnectionStatusMessageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtIssuerFilter.class)
)
@AutoConfigureMockMvc(addFilters = false) // disables spring security filters
@Import(ConnectionStatusMessageControllerTest.TestConfig.class)
class ConnectionStatusMessageControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ConnectionStatusMessageController controller;
    @Autowired
    private MessageStreamHub messageStreamHub;

    @Test
    void connectionStatusMessageByPermissionIds_sendsStatus() {
        var message1 = statusMessage("1", PermissionProcessStatus.CREATED);
        var message2 = statusMessage("2", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        var message3 = statusMessage("3", PermissionProcessStatus.VALIDATED);
        TestPublisher<ConnectionStatusMessage> publisher = TestPublisher.create();
        messageStreamHub.registerProvider(ConnectionStatusMessage.class, publisher::flux);
        publisher.next(message1, message2, message3);
        publisher.complete();

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
                    .thenCancel()
                    .verify();
    }

    private ConnectionStatusMessage statusMessage(String pid, PermissionProcessStatus status) {
        return new ConnectionStatusMessage(
                "1",
                pid,
                "1",
                null,
                status);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        MessageStreamHub messageStreamHub() {
            return new MessageStreamHub();
        }

        @Bean
        StreamProviderAndSupplierRegistrar streamProviderAndSupplierRegistrar() {
            return new StreamProviderAndSupplierRegistrar();
        }
    }
}
