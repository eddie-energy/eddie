// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.web;

import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.exceptions.UnauthorizedException;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestCreationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorizationCallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthorizationCallbackControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestAuthorizationService authorizationService;
    @MockitoBean
    private PermissionRequestCreationService creationService;

    @Test
    void callback_withError_returnsThymeleafTemplateWithError() throws Exception {
        // Given
        doThrow(MissingClientIdException.class)
                .when(authorizationService).authorizePermissionRequest(any());

        // When
        mockMvc.perform(get("/authorization-callback")
                                .queryParam("state", "pid")
                                .queryParam("error", "asdf"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "ERROR"));
    }

    @Test
    void callback_withoutAuthorization_returnsThymeleafTemplateWithDenied() throws Exception {
        // Given
        doThrow(UnauthorizedException.class)
                .when(authorizationService).authorizePermissionRequest(any());

        // When
        mockMvc.perform(get("/authorization-callback")
                                .queryParam("state", "pid")
                                .queryParam("code", "asdf"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "DENIED"));
    }

    @Test
    void callback_successful_returnsDataNeedId() throws Exception {
        // Given
        when(creationService.findDataNeedIdByPermissionId("pid"))
                .thenReturn(Optional.of("dnid"));

        // When
        mockMvc.perform(get("/authorization-callback")
                                .queryParam("state", "pid")
                                .queryParam("code", "asdf"))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "OK"))
               .andExpect(model().attribute("dataNeedId", "dnid"));
    }
}