// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.regionconnector.es.datadis.CimTestConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.es.datadis.health.DatadisApiHealthIndicator;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionEventRepository;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Set;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.connectionStatusMessagesStreamFor;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionController.class}, properties = {"eddie.jwt.hmac.secret=RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=", "eddie.permission.request.timeout.duration=24"})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
@SuppressWarnings("unused")
@Import(CimTestConfiguration.class)
class PermissionControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestService mockService;
    @MockitoBean
    private EsPermissionRequestRepository unusedMockRepository;
    @MockitoBean
    private EsPermissionEventRepository unusedPermissionEventRepository;
    @MockitoBean
    private DatadisApiHealthIndicator healthIndicator;

    @Test
    void acceptPermission_permissionExists_returnsOk_andCallsService() throws Exception {
        // Given
        String permissionId = "ValidId";

        // When
        mockMvc.perform(patch("/permission-request/accepted")
                                .queryParam("permission-id", permissionId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk());

        verify(mockService).acceptPermission(Set.of(permissionId));
    }

    @Test
    void acceptPermission_permissionDoesNotExist_returnsResultWithoutPermission() throws Exception {
        // Given
        String nonExistingId = "123";
        when(mockService.acceptPermission(any()))
                .thenReturn(Set.of());

        // When
        mockMvc.perform(patch("/permission-request/accepted")
                                .queryParam("permission-id", nonExistingId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", iterableWithSize(0)));
    }

    @Test
    void rejectPermission_permissionExists_returnsOk_andCallsService() throws Exception {
        // Given
        String permissionId = "ValidId";

        // When
        mockMvc.perform(patch("/permission-request/rejected")
                                .queryParam("permission-id", permissionId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk());

        verify(mockService).rejectPermission(Set.of(permissionId));
    }

    @Test
    void rejectPermission_permissionDoesNotExist_returnsResultWithoutPermission() throws Exception {
        // Given
        String nonExistingId = "123";
        when(mockService.rejectPermission(any())).thenReturn(Set.of());

        // When
        mockMvc.perform(patch("/permission-request/rejected")
                                .queryParam("permission-id", nonExistingId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", iterableWithSize(0)));
    }

    @Test
    void requestPermission_missingRequiredFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("{\"connectionId\": \"Hello World\"}"))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(3),
                       hasItem("nif: must not be blank"),
                       hasItem("meteringPointId: must not be blank"),
                       hasItem("dataNeedIds: must not be empty")
               )));
    }

    @Test
    void requestPermission_invalidDataNeedsField_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                // language=JSON
                                .content(
                                        "{\"connectionId\": \"Hello World\", \"dataNeedIds\": [null, \"\"], \"meteringPointId\":  \"mid\", \"nif\": \"NOICE\"}"))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(2),
                       hasItem("dataNeedIds[]: must not be blank"),
                       hasItem("dataNeedIds[]: must not be blank")
               )));
    }

    @Test
    void requestPermission_noBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void requestPermission_wrongContentType_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void requestPermission_blankOrEmptyFields_returnsBadRequest() throws Exception {
        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "   ")
                                    .put("meteringPointId", "   ")
                                    .put("nif", "");
        jsonNode.putArray("dataNeedIds");

        mockMvc.perform(post("/permission-request")
                                .content(mapper.writeValueAsString(jsonNode))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
               .andDo(print())
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(4),
                       hasItem("connectionId: must not be blank"),
                       hasItem("nif: must not be blank"),
                       hasItem("meteringPointId: must not be blank"),
                       hasItem("dataNeedIds: must not be empty")
               )));
    }

    @Test
    void requestPermission_validInput_returnsCreatedAndSetsHeader() throws Exception {
        // Given
        var testPermissionId = "MyTestId";
        when(mockService.createAndSendPermissionRequest(any()))
                .thenReturn(new CreatedPermissionRequest(testPermissionId));


        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "ConnId")
                                    .put("meteringPointId", "SomeId")
                                    .put("nif", "NOICE");
        jsonNode
                .putArray("dataNeedIds")
                .add("SomeId");

        // When
        var expectedUri = connectionStatusMessagesStreamFor(testPermissionId).toString();
        var content = mapper.writeValueAsString(jsonNode);
        mockMvc.perform(post("/permission-request")
                                .content(content)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.permissionIds[0]").value(testPermissionId))
               .andExpect(header().string(HttpHeaders.LOCATION, expectedUri));

        verify(mockService).createAndSendPermissionRequest(any());
    }

    /**
     * The {@link RegionConnectorsCommonControllerAdvice} is automatically registered for each region connector when the
     * whole core is started. To be able to properly test the controller's error responses, manually add the advice to
     * this test class.
     */
    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
        }

        @Bean
        public JwtUtil jwtUtil(
                @Value("${eddie.jwt.hmac.secret}") String jwtHmacSecret,
                @Value("${eddie.permission.request.timeout.duration}") int timeoutDuration
        ) {
            return new JwtUtil(jwtHmacSecret, timeoutDuration);
        }
    }
}
