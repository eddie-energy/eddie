// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.web;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.exceptions.NlValidationException;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PermissionRequestService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URISyntaxException;
import java.security.PrivateKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(ControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class ControllerAdviceTest {
    @MockitoBean
    private PkceClientController controller;
    @MockitoBean
    @SuppressWarnings("unused")
    private PrivateKey ignored;
    @MockitoBean
    @SuppressWarnings("unused")
    private PermissionEventRepository ignoredEventRepo;
    @MockitoBean
    @SuppressWarnings("unused")
    private NlPermissionRequestRepository ignoredPermissionRequestRepo;
    @MockitoBean
    @SuppressWarnings("unused")
    private PermissionRequestService ignoredPermissionRequestService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testURISyntaxExceptionAdvice() throws Exception {
        // Given
        when(controller.callback(any(), any()))
                .thenThrow(URISyntaxException.class);

        // When
        mockMvc.perform(get("/oauth2/code/mijn-aansluiting")
                                .cookie(new Cookie("EDDIE-SESSION-ID", "asdf")))
               // Then
               .andExpect(status().isBadRequest());
    }

    @Test
    void testNlValidationException_returnsBadRequest() throws Exception {
        // Given
        when(controller.permissionRequest(any(), any()))
                .thenThrow(new NlValidationException(new AttributeError("postalCode", "msg")));
        var content = new PermissionRequestForCreation("cid", "dnid", "11");

        // When
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(content)))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.postalCode.[0].message").value("msg"));
    }
}