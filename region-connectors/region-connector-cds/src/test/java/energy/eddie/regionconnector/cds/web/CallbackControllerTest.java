package energy.eddie.regionconnector.cds.web;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.services.oauth.CallbackService;
import energy.eddie.regionconnector.cds.services.oauth.authorization.AcceptedResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.ErrorResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.UnauthorizedResult;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class CallbackControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CallbackService callbackService;

    @Test
    void callback_withPermissionNotFound_returnsThymeleafTemplateWithError() throws Exception {
        // Given
        when(callbackService.processCallback(any())).thenThrow(PermissionNotFoundException.class);

        // When
        mockMvc.perform(get("/callback")
                                .queryParam("state", "pid")
                                .queryParam("error", "asdf"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "ERROR"));
    }

    @Test
    void callback_withError_returnsThymeleafTemplateWithError() throws Exception {
        // Given
        when(callbackService.processCallback(any()))
                .thenReturn(new ErrorResult("pid", "error"));

        // When
        mockMvc.perform(get("/callback")
                                .queryParam("state", "pid")
                                .queryParam("error", "asdf"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "ERROR"));
    }

    @Test
    void callback_withRejected_returnsThymeleafTemplateWithDenied() throws Exception {
        // Given
        when(callbackService.processCallback(any()))
                .thenReturn(new UnauthorizedResult("pid", PermissionProcessStatus.REJECTED));

        // When
        mockMvc.perform(get("/callback")
                                .queryParam("state", "pid")
                                .queryParam("error", "access_denied"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "DENIED"));
    }

    @Test
    void callback_successful_returnsDataNeedId() throws Exception {
        // Given
        when(callbackService.processCallback(any()))
                .thenReturn(new AcceptedResult("pid", "dnid"));

        // When
        mockMvc.perform(get("/callback")
                                .queryParam("state", "pid")
                                .queryParam("code", "asdf"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "OK"))
               .andExpect(model().attribute("dataNeedId", "dnid"));
    }
}