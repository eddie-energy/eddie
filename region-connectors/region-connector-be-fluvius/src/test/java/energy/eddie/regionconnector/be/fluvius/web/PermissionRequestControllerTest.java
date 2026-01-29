// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.web;

import energy.eddie.regionconnector.be.fluvius.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.service.AcceptanceOrRejectionService;
import energy.eddie.regionconnector.be.fluvius.service.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriTemplate;
import tools.jackson.databind.ObjectMapper;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@Import(PermissionRequestController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestService service;
    @MockitoBean
    private AcceptanceOrRejectionService acceptanceOrRejectionService;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws Exception {
        // Given
        var expectedLocationHeader = new UriTemplate(CONNECTION_STATUS_STREAM).expand("pid").toString();
        var created = new CreatedPermissionRequest("pid");
        when(service.createPermissionRequest(any())).thenReturn(created);
        var jsonNode = objectMapper.createObjectNode()
                                   .put("connectionId", "cid")
                                   .put("dataNeedId", "dnid")
                                   .put("flow", "B2B");

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(jsonNode.toString())
               )
               // Then
               .andExpect(status().isCreated())
               .andExpect(content().json(objectMapper.writeValueAsString(created)))
               .andExpect(header().string("Location", is(expectedLocationHeader)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"accepted", "rejected"})
    void testCallbackEndpoints_returnError_onUnknownPermissionRequest(String status) throws Exception {
        // Given
        doThrow(PermissionNotFoundException.class)
                .when(acceptanceOrRejectionService).acceptOrRejectPermissionRequest(any(), any());

        // When
        mockMvc.perform(get("/permission-request/{pid}/{status}", "pid", status))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "ERROR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"accepted", "rejected"})
    void testCallbackEndpoints_returnAccepted_onAcceptedPermissionRequest(String status) throws Exception {
        // Given
        when(acceptanceOrRejectionService.acceptOrRejectPermissionRequest(any(), any())).thenReturn(true);

        // When
        mockMvc.perform(get("/permission-request/{pid}/{status}", "pid", status))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "OK"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"accepted", "rejected"})
    void testCallbackEndpoints_returnRejected_onRejectedPermissionRequest(String status) throws Exception {
        // Given
        when(acceptanceOrRejectionService.acceptOrRejectPermissionRequest(any(), any())).thenReturn(false);

        // When
        mockMvc.perform(get("/permission-request/{pid}/{status}", "pid", status))
               // Then
               .andExpect(status().isOk())
               .andExpect(view().name("authorization-callback"))
               .andExpect(model().attribute("status", "DENIED"));
    }
}