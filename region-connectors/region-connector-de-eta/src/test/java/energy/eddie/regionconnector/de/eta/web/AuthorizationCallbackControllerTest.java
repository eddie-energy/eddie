package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.regionconnector.de.eta.auth.AuthCallback;
import energy.eddie.regionconnector.de.eta.service.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthorizationCallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthorizationCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionRequestAuthorizationService authorizationService;

    @Test
    void callback_successful_returnsOk() throws Exception {
        // Given
        doNothing().when(authorizationService).authorizePermissionRequest(any(AuthCallback.class));

        // When
        mockMvc.perform(get("/authorization-callback")
                                .queryParam("state", "pid")
                                .queryParam("token", "tokenValue"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "OK"));
    }

    @Test
    void callback_withPermissionNotFound_returnsError() throws Exception {
        // Given
        doThrow(PermissionNotFoundException.class).when(authorizationService)
                                                  .authorizePermissionRequest(any(AuthCallback.class));

        // When
        mockMvc.perform(get("/authorization-callback")
                                .queryParam("state", "pid")
                                .queryParam("token", "tokenValue"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "ERROR"));
    }

    @Test
    void callback_withError_returnsError() throws Exception {
        // Given
        doThrow(RuntimeException.class).when(authorizationService).authorizePermissionRequest(any(AuthCallback.class));

        // When
        mockMvc.perform(get("/authorization-callback")
                                .queryParam("state", "pid")
                                .queryParam("error", "some_error"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "ERROR"));
    }
}
