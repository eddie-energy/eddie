// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.config.OAuth2SecurityConfiguration;
import energy.eddie.aiida.dtos.provisioning.ProvisioningConnectionDto;
import energy.eddie.aiida.services.ProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProvisioningController.class)
@Import(OAuth2SecurityConfiguration.class)
class ProvisioningControllerSecurityIntegrationTest {
    private static final UUID PERMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProvisioningService provisioningService;

    @Test
    void getProvisioningTypes_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/provisioning/types"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void getProvisioningTypes_withAuthentication_returnsTypes() throws Exception {
        mockMvc.perform(get("/provisioning/types").with(jwt()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("provisioningTypes").isArray());
    }

    @Test
    void patchProvisioningType_withAuthenticationWithoutCsrf_reachesController() throws Exception {
        when(provisioningService.changeProvisioningType(eq(PERMISSION_ID), any()))
                .thenReturn(ProvisioningConnectionDto.empty());

        mockMvc.perform(validPatch().with(jwt()))
               .andExpect(status().isOk());
    }

    @Test
    void patchProvisioningType_withInvalidEnum_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/provisioning/permission/" + PERMISSION_ID + "/patchInboundProvisioning")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                 {
                                                   "permissionId": "00000000-0000-0000-0000-000000000001",
                                                   "type": "NOT_A_PROVISIONING_TYPE"
                                                 }
                                                 """))
               .andExpect(status().isBadRequest());
    }

    private static MockHttpServletRequestBuilder validPatch() {
        return patch("/provisioning/permission/" + PERMISSION_ID + "/patchInboundProvisioning")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                 {
                                   "permissionId": "00000000-0000-0000-0000-000000000001",
                                   "type": "REST_BEARER"
                                 }
                                 """);
    }
}
