// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.web;

import energy.eddie.regionconnector.us.green.button.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.us.green.button.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestCreationService;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PermissionRequestController.class)
@Import(PermissionRequestController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestCreationService creationService;
    @SuppressWarnings("unused")
    @MockitoBean
    private PermissionRequestAuthorizationService authorizationService;

    @Test
    void createPermissionRequest_returnsCreatedPermissionRequest() throws Exception {
        // Given
        when(creationService.createPermissionRequest(any()))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("http://localhost")));
        var pr = new PermissionRequestForCreation("cid", "dnid", "http://localhost", "company", "US");

        // When
        mockMvc.perform(
                       post("/permission-request")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(objectMapper.writeValueAsString(pr))
               )
               // Then
               .andExpect(header().exists("Location"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.redirectUri", is("http://localhost")));
    }
}
