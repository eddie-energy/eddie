package energy.eddie.aiida.web;

import energy.eddie.aiida.config.OAuth2SecurityConfiguration;
import energy.eddie.aiida.errors.MqttUnauthorizedException;
import energy.eddie.aiida.models.datasource.MqttAction;
import energy.eddie.aiida.services.MqttAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MqttAuthController.class)
@Import(OAuth2SecurityConfiguration.class)
class MqttAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private MqttAuthService service;

    @Test
    void authenticated_shouldReturnOk() throws Exception {
        doNothing().when(service).isAuthenticatedOrThrow("admin", "admin");

        mockMvc.perform(post("/mqtt-auth/auth")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .content("username=user&password=pass"))
               .andExpect(status().isOk());

        verify(service).isAuthenticatedOrThrow("user", "pass");
    }

    @Test
    void authenticated_shouldReturnUnauthorized() throws Exception {
        doThrow(new MqttUnauthorizedException("unauthorized")).when(service).isAuthenticatedOrThrow("user", "pass");

        mockMvc.perform(post("/mqtt-auth/auth")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .content("username=user&password=pass"))
               .andExpect(status().isUnauthorized());

        verify(service).isAuthenticatedOrThrow("user", "pass");
    }

    @Test
    void authenticated_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/mqtt-auth/auth")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .content("username=user"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void superuser_shouldReturnOk() throws Exception {
        doNothing().when(service).isAuthenticatedOrThrow("admin", "admin");

        mockMvc.perform(post("/mqtt-auth/superuser")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .content("username=user&password=pass"))
               .andExpect(status().isOk());

        verify(service).isAdminOrThrow("user", "pass");
    }

    @Test
    void superuser_shouldReturnUnauthorized() throws Exception {
        doThrow(new MqttUnauthorizedException("unauthorized")).when(service).isAdminOrThrow("user", "pass");

        mockMvc.perform(post("/mqtt-auth/superuser")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .content("username=user&password=pass"))
               .andExpect(status().isUnauthorized());

        verify(service).isAdminOrThrow("user", "pass");
    }

    @Test
    void acl_shouldReturnOk() throws Exception {
        doNothing().when(service).isAuthenticatedOrThrow("admin", "admin");

        mockMvc.perform(post("/mqtt-auth/acl")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .content("username=user&password=pass&action=2&topic=topic"))
               .andExpect(status().isOk());

        verify(service).isAuthorizedOrThrow("user", "pass", MqttAction.PUBLISH, "topic");
    }

    @Test
    void acl_shouldReturnUnauthorized() throws Exception {
        doThrow(new MqttUnauthorizedException("unauthorized")).when(service).isAuthorizedOrThrow("user", "pass", MqttAction.SUBSCRIBE, "topic");

        mockMvc.perform(post("/mqtt-auth/acl")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .content("username=user&password=pass&action=1&topic=topic"))
               .andExpect(status().isUnauthorized());

        verify(service).isAuthorizedOrThrow("user", "pass", MqttAction.SUBSCRIBE, "topic");
    }
}
