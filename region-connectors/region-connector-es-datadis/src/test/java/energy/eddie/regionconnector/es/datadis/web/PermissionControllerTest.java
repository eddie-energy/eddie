package energy.eddie.regionconnector.es.datadis.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.CimTestConfiguration;
import energy.eddie.regionconnector.es.datadis.DatadisSpringConfig;
import energy.eddie.regionconnector.es.datadis.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.es.datadis.health.DatadisApiHealthIndicator;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisDataSourceInformation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionEventRepository;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PermissionController.class}, properties = {"eddie.jwt.hmac.secret=RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=", "eddie.permission.request.timeout.duration=24"})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
@SuppressWarnings("unused")
@Import(CimTestConfiguration.class)
class PermissionControllerTest {
    private final ObjectMapper mapper = new DatadisSpringConfig().objectMapper();
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
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedsService dataNeedsService;

    @Test
    void permissionStatus_permissionExists_returnsOk() throws Exception {
        // Given
        var datadisDataSourceInformation = new DatadisDataSourceInformation(mock(EsPermissionRequest.class));
        String permissionId = "ValidId";
        var statusMessage = new ConnectionStatusMessage("cid",
                                                        permissionId,
                                                        "dnid",
                                                        datadisDataSourceInformation,
                                                        PermissionProcessStatus.ACCEPTED);
        when(mockService.findConnectionStatusMessageById(anyString())).thenReturn(Optional.of(statusMessage));

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/permission-status/{permissionId}", permissionId)
                                              .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permissionId", is(permissionId)));
    }

    @Test
    void permissionStatus_permissionDoesNotExist_returnsNotFound() throws Exception {
        // Given

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/permission-status/{permissionId}", "123")
                                              .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No permission with ID '123' found.")));
    }

    @Test
    void acceptPermission_permissionExists_returnsOk_andCallsService() throws Exception {
        // Given
        String permissionId = "ValidId";

        // When
        mockMvc.perform(patch("/permission-request/{permissionId}/accepted", permissionId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk());

        verify(mockService).acceptPermission(permissionId);
    }

    @Test
    void acceptPermission_permissionDoesNotExist_returnsNotFound() throws Exception {
        // Given
        String nonExistingId = "123";
        var ex = new PermissionNotFoundException(nonExistingId);
        doThrow(ex).when(mockService).acceptPermission(anyString());

        // When
        mockMvc.perform(patch("/permission-request/{permissionId}/accepted", nonExistingId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No permission with ID '123' found.")));
    }

    @Test
    void rejectPermission_permissionExists_returnsOk_andCallsService() throws Exception {
        // Given
        String permissionId = "ValidId";

        // When
        mockMvc.perform(patch("/permission-request/{permissionId}/rejected", permissionId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk());

        verify(mockService).rejectPermission(permissionId);
    }

    @Test
    void rejectPermission_permissionDoesNotExist_returnsNotFound() throws Exception {
        // Given
        String nonExistingId = "123";
        var ex = new PermissionNotFoundException(nonExistingId);
        doThrow(ex).when(mockService).rejectPermission(anyString());

        // When
        mockMvc.perform(patch("/permission-request/{permissionId}/rejected", nonExistingId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No permission with ID '123' found.")));
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
                       hasItem("nif: must not be null or blank"),
                       hasItem("meteringPointId: must not be null or blank"),
                       hasItem("dataNeedId: must not be null or blank")
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
                                    .put("dataNeedId", "")
                                    .put("nif", "");

        mockMvc.perform(post("/permission-request")
                                .content(mapper.writeValueAsString(jsonNode))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
               .andDo(print())
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(4),
                       hasItem("connectionId: must not be null or blank"),
                       hasItem("nif: must not be null or blank"),
                       hasItem("meteringPointId: must not be null or blank"),
                       hasItem("dataNeedId: must not be null or blank")
               )));
    }

    @Test
    void requestPermission_validInput_returnsCreatedAndSetsHeader() throws Exception {
        // Given
        var testPermissionId = "MyTestId";
        when(mockService.createAndSendPermissionRequest(any())).thenReturn(new CreatedPermissionRequest(testPermissionId,
                                                                                                        ""));


        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "ConnId")
                                    .put("meteringPointId", "SomeId")
                                    .put("dataNeedId", "BLA_BLU_BLE")
                                    .put("nif", "NOICE");

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/permission-request")
                                                                   .content(mapper.writeValueAsString(jsonNode))
                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                   .accept(MediaType.APPLICATION_JSON))
                                                  // Then
                                                  .andExpect(status().isCreated())
                                                  .andExpect(jsonPath("$.permissionId").value(testPermissionId))
                                                  .andReturn().getResponse();

        assertEquals("/permission-status/MyTestId", response.getHeader("Location"));
        verify(mockService).createAndSendPermissionRequest(any());
    }

    @Test
    void givenJwtCreationFailedException_returnsInternalServerError() throws Exception {
        // Given
        var testPermissionId = "MyTestId";
        when(mockService.createAndSendPermissionRequest(any())).thenThrow(mock(JwtCreationFailedException.class));

        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "ConnId")
                                    .put("meteringPointId", "SomeId")
                                    .put("dataNeedId", "BLA_BLU_BLE")
                                    .put("nif", "NOICE");

        mockMvc.perform(post("/permission-request").content(mapper.writeValueAsString(jsonNode))
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isInternalServerError());
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
