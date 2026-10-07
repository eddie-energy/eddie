// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.web;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.CimTestConfiguration;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionEventRepository;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthorizationCallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CimTestConfiguration.class)
class AuthorizationCallbackControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestService permissionRequestService;
    @MockitoBean
    @SuppressWarnings("unused")
    private FrPermissionRequestRepository unusedRepository;
    @MockitoBean
    @SuppressWarnings("unused")
    private FrPermissionEventRepository permissionEventRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedsService dataNeedsService;

    @Test
    void authorizationCallback_withParams_returnsAttributes() throws Exception {
        // Given
        doNothing().when(permissionRequestService).authorizePermissionRequest(anyString(), anyLong());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("state", UUID.randomUUID().toString())
                                             .param("autorisation_id", "88482")
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(model().attribute("status", "OK"));
    }

    @Test
    void authorizationCallback_noAuthorizationId_returnsDenied() throws Exception {
        // Given
        doNothing().when(permissionRequestService).authorizePermissionRequest(anyString(), anyLong());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("state", UUID.randomUUID().toString())
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(model().attribute("status", "DENIED"));
    }

    @Test
    void authorizationCallback_noState_returnsDenied() throws Exception {
        // Given
        doNothing().when(permissionRequestService).authorizePermissionRequest(anyString(), anyLong());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("autorisation_id", "88482")
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(model().attribute("status", "DENIED"));
    }

    @Test
    void authorizationCallback_withInvalidPermission_returnsError() throws Exception {
        // Given
        doThrow(new PermissionNotFoundException("")).when(permissionRequestService)
                                                    .authorizePermissionRequest(anyString(), anyLong());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("state", UUID.randomUUID().toString())
                                             .param("autorisation_id", "invalid")
               )
               // Then
               .andExpect(status().isBadRequest());
    }
}
