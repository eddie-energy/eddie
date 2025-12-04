package energy.eddie.regionconnector.de.eta.oauth;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.regionconnector.de.eta.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.InvalidEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeEtaOAuthCallbackController.class)
@Import(DeEtaOAuthCallbackControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class DeEtaOAuthCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeEtaOAuthStateStore stateStore;

    @MockBean
    private DeEtaOAuthTokenService tokenService;

    @MockBean
    private Outbox outbox;

    @Test
    void callback_success_emitsAcceptedEvent() throws Exception {
        // given
        var permissionId = UUID.randomUUID();
        var state = UUID.randomUUID().toString();
        stateStore.save(permissionId, "conn-1", state);

        var token = new DeEtaOAuthToken("conn-1", "enc-a", "enc-r", java.time.LocalDateTime.now().plusHours(1), "scope1");
        var result = new DeEtaOAuthTokenService.Result(token, permissionId.toString());

        org.mockito.Mockito.when(tokenService.exchangeAuthorizationCode(anyString(), anyString())).thenReturn(result);

        // when
        mockMvc.perform(get("/region-connectors/de-eta/callback")
                        .param("code", "auth_code")
                        .param("state", state))
                .andExpect(status().isNoContent());

        // then
        ArgumentCaptor<PermissionEvent> captor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue(), instanceOf(AcceptedEvent.class));
    }

    @Test
    void callback_failure_emitsInvalidEvent() throws Exception {
        // given
        var permissionId = UUID.randomUUID();
        var state = UUID.randomUUID().toString();
        stateStore.save(permissionId, "conn-1", state);

        doThrow(new RuntimeException("boom")).when(tokenService).exchangeAuthorizationCode(anyString(), anyString());

        // when
        mockMvc.perform(get("/region-connectors/de-eta/callback")
                        .param("code", "auth_code")
                        .param("state", state))
                .andExpect(status().isBadRequest());

        // then
        ArgumentCaptor<PermissionEvent> captor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue(), instanceOf(InvalidEvent.class));
    }

    static class TestConfig {
        @Bean
        DeEtaOAuthStateStore stateStore() {
            return new DeEtaOAuthStateStore();
        }
    }
}
