// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.web;

import energy.eddie.regionconnector.cds.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.cds.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.services.PermissionRequestCreationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@Import({PermissionRequestController.class, PermissionRequestControllerAdvice.class})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestCreationService creationService;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws Exception {
        // Given
        var expectedLocationHeader = new UriTemplate(CONNECTION_STATUS_STREAM)
                .expand("pid")
                .toString();
        var pr = new PermissionRequestForCreation(1L, "dnid", "cid");
        when(creationService.createPermissionRequest(pr))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("urn:example")));


        // When
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pr)))
               // Then
               .andExpect(header().string("Location", is(expectedLocationHeader)))
               .andExpect(content().json("{\"permissionId\":  \"pid\", \"redirectUri\": \"urn:example\"}"));
    }

    @Test
    void testCreatePermissionRequest_withUnknownCdsServer_returnsBadRequest() throws Exception {
        // Given
        var pr = new PermissionRequestForCreation(1L, "dnid", "cid");
        when(creationService.createPermissionRequest(pr))
                .thenThrow(new UnknownPermissionAdministratorException(1L));

        // When
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pr)))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].message", equalTo("Unknown permission administrator: 1")));
    }
}