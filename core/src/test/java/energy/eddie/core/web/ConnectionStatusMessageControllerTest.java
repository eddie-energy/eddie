package energy.eddie.core.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.core.services.PermissionService;
import energy.eddie.dataneeds.web.DataNeedsController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionStatusMessageController.class)
@AutoConfigureMockMvc(addFilters = false) // disables spring security filters
public class ConnectionStatusMessageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PermissionService permissionService;
    @Qualifier("webTestClient")
    @Autowired
    private WebTestClient webTestClient;

    private ConnectionStatusMessage statusMessage(PermissionProcessStatus status) {
        return new ConnectionStatusMessage(
                "1",
                "1",
                "1",
                null,
                status);
    }

    @Test
    public void connectionStatusMessageByPermissionId_sendsStatus() throws Exception {
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
}
