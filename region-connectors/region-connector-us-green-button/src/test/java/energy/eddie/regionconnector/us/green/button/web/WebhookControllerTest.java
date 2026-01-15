package energy.eddie.regionconnector.us.green.button.web;

import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvents;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.us.green.button.services.utility.events.UtilityEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookController.class)
@Import(WebhookController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class WebhookControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    @SuppressWarnings("unused")
    private UtilityEventService service;
    @SuppressWarnings("unused")
    @MockitoBean
    private PermissionRequestAuthorizationService authorizationService;

    @Test
    void testWebhook_receivesEvents() throws Exception {
        // Given
        var events = new WebhookEvents(
                List.of(
                        new WebhookEvent(
                                "uid",
                                "authorization_expired",
                                ZonedDateTime.now(ZoneOffset.UTC),
                                "webhook",
                                URI.create("http://localhost"),
                                false,
                                "0000",
                                null
                        )
                ),
                null
        );

        // When
        mockMvc.perform(
                post("/webhook")
                        .header("X-UtilityAPI-Webhook-Signature",
                                "3df0899877ec31ee531ef34d8c46d1baf97c75ef96ec4290af4d4836e42aa0ea")
                        .header("X-UtilityAPI-Webhook-Salt", "salt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(events))
                // Then
        ).andExpect(status().isOk());
    }
}