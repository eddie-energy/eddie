package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PermissionController.class})
class PermissionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ConfigurableEnvironment environment;
    @MockBean
    private PermissionRequestService mockService;

    @BeforeEach
    void setUp() {
        environment.setActiveProfiles();
    }

    @Test
    void javascriptConnectorElement_returnsOk() throws Exception {
        // Given

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/es-datadis/ce.js"))
                // Then
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void javascriptConnectorElement_returnsOk_withDevProfile() throws Exception {
        // Given
        environment.setActiveProfiles("dev");

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/es-datadis/ce.js"))
                // Then
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void permissionStatus_permissionExists_returnsOk() throws Exception {
        // Given
        var state = new AcceptedState(null);
        var statusMessage = new ConnectionStatusMessage("cid", "ValidId", "dnid", state.status());
        when(mockService.findConnectionStatusMessageById(anyString())).thenReturn(Optional.of(statusMessage));

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/es-datadis/permission-status")
                        .param("permissionId", "ValidId")
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk());
    }

    @Test
    void permissionStatus_permissionDoesNotExist_returnsNotFound() throws Exception {
        // Given

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/es-datadis/permission-status")
                        .param("permissionId", "123")
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptPermission_permissionExists_returnsOk_andCallsService() throws Exception {
        // Given
        String permissionId = "ValidId";

        // When
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request/accepted")
                        .param("permissionId", permissionId)
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
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request/accepted")
                        .param("permissionId", nonExistingId)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("No permission with ID 123 found"))));
    }

    @Test
    void rejectPermission_permissionExists_returnsOk_andCallsService() throws Exception {
        // Given
        String permissionId = "ValidId";

        // When
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request/rejected")
                        .param("permissionId", permissionId)
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
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request/rejected")
                        .param("permissionId", nonExistingId)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("No permission with ID 123 found"))));
    }

    @Test
    void requestPermission_missingAllRequiredFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .param("useless", "value")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(5),
                        hasItem("connectionId must not be null or blank"),
                        hasItem("nif must not be null or blank"),
                        hasItem("meteringPointId must not be null or blank"),
                        hasItem("dataNeedId must not be null or blank"),
                        hasItem("measurementType must not be null")
                )));
    }

    @Test
    void requestPermission_missingMeasurementType_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .param("connectionId", "foo")
                        .param("meteringPointId", "bar")
                        .param("dataNeedId", "du")
                        .param("nif", "muh")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("measurementType must not be null")
                )));
    }

    @Test
    void requestPermission_invalidMeasurementType_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .param("connectionId", "foo")
                        .param("meteringPointId", "bar")
                        .param("dataNeedId", "du")
                        .param("nif", "muh")
                        .param("measurementType", "INVALID")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]", containsString("Failed to convert")))
                .andExpect(jsonPath("$.errors[0]", containsString("MeasurementType")));
    }

    // TODO write more tests for controller permission-request
}