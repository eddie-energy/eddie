// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web.webhook;

import energy.eddie.aiida.web.webhook.dtos.ClientConnAckRequest;
import energy.eddie.aiida.web.webhook.dtos.ClientDisconnectedRequest;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = WebhookController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class WebhookControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private LogCaptor logCaptor;

    @BeforeEach
    void setUp() {
        logCaptor = LogCaptor.forClass(WebhookController.class);
        logCaptor.setLogLevelToDebug();
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    @WithMockUser
    void testClientConnackRequest() throws Exception {
        // Given
        var clientConnackPayload = """
                {
                    "proto_ver":4,
                    "keepalive":60,
                    "conn_ack":"success",
                    "username":"wbhk_test",
                    "clientid":"wbhk_clientid",
                    "action":"client_connack"
                }
                """;
        var clientConnackRequest = objectMapper.readValue(clientConnackPayload, ClientConnAckRequest.class);

        // When
        mockMvc.perform(post("/webhook/event").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(clientConnackPayload))
               .andExpect(status().isOk());

        assertTrue(logCaptor.getDebugLogs()
                            .contains("Received event %s with status %s from client %s".formatted(clientConnackRequest.action(),
                                                                                                  clientConnackRequest.connAck(),
                                                                                                  clientConnackRequest.clientId())));
    }

    @Test
    @WithMockUser
    void testClientDisconnectedRequest() throws Exception {
        // Given
        var clientDisconnectedPayload = """
                {
                    "reason":"normal",
                    "username":"wbhk_test",
                    "clientid":"wbhk_clientid",
                    "action":"client_disconnected"
                }
                """;
        var clientDisconnectedRequest = objectMapper.readValue(clientDisconnectedPayload,
                                                               ClientDisconnectedRequest.class);

        // When
        mockMvc.perform(post("/webhook/event").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                                              .content(clientDisconnectedPayload)).andExpect(status().isOk());

        assertTrue(logCaptor.getDebugLogs()
                            .contains("Received event %s with reason %s from client %s".formatted(
                                    clientDisconnectedRequest.action(),
                                    clientDisconnectedRequest.reason(),
                                    clientDisconnectedRequest.clientId())));
    }

    @Test
    @WithMockUser
    void testUnsupportedEvent() throws Exception {
        // Given
        var clientDisconnectedPayload = """
                {
                    "reason":"normal",
                    "username":"wbhk_test",
                    "clientid":"wbhk_clientid",
                    "action":"unsupported_event"
                }
                """;

        // When, Then
        mockMvc.perform(post("/webhook/event").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                                              .content(clientDisconnectedPayload)).andExpect(status().isBadRequest());
    }
}
