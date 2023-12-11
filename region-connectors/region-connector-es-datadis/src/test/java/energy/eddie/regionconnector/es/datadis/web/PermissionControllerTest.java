package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisRegionalInformation;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PermissionController.class})
class PermissionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionRequestService mockService;

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
    void permissionStatus_permissionExists_returnsOk() throws Exception {
        // Given
        var state = new AcceptedState(null);
        var regionalInformation = new DatadisRegionalInformation();
        var statusMessage = new ConnectionStatusMessage("cid", "ValidId", "dnid", regionalInformation, state.status());
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
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(7),
                        hasItem("connectionId must not be null or blank"),
                        hasItem("nif must not be null or blank"),
                        hasItem("meteringPointId must not be null or blank"),
                        hasItem("dataNeedId must not be null or blank"),
                        hasItem("measurementType must not be null"),
                        hasItem("requestDataFrom must not be null"),
                        hasItem("requestDataTo must not be null")
                )));
    }

    @Test
    void requestPermission_missingMeasurementType_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .param("connectionId", "foo")
                        .param("meteringPointId", "bar")
                        .param("dataNeedId", "du")
                        .param("nif", "muh")
                        .param("requestDataFrom", "2023-09-09")
                        .param("requestDataTo", "2023-10-10")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
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
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]", containsString("Failed to convert")))
                .andExpect(jsonPath("$.errors[0]", containsString("MeasurementType")));
    }

    @Test
    void requestPermission_noBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPermission_wrongContentType_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void requestPermission_blankOrEmptyFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .param("connectionId", "   ")
                        .param("meteringPointId", "   ")
                        .param("dataNeedId", "")
                        .param("nif", "")
                        .param("measurementType", "QUARTER_HOURLY")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(6),
                        hasItem("connectionId must not be null or blank"),
                        hasItem("nif must not be null or blank"),
                        hasItem("meteringPointId must not be null or blank"),
                        hasItem("dataNeedId must not be null or blank"),
                        hasItem("requestDataFrom must not be null"),
                        hasItem("requestDataTo must not be null")
                )));
    }

    @Test
    void requestPermission_validInput_returnsCreatedAndSetsHeader() throws Exception {
        // Given
        var testPermissionId = "MyTestId";
        var mockPermissionRequest = mock(PermissionRequest.class);
        when(mockPermissionRequest.permissionId()).thenReturn(testPermissionId);
        when(mockService.createAndSendPermissionRequest(any())).thenReturn(mockPermissionRequest);

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/region-connectors/es-datadis/permission-request")
                        .param("connectionId", "ConnId")
                        .param("meteringPointId", "SomeId")
                        .param("dataNeedId", "BLA_BLU_BLE")
                        .param("nif", "NOICE")
                        .param("measurementType", "QUARTER_HOURLY")
                        .param("requestDataFrom", "2023-09-09")
                        .param("requestDataTo", "2023-10-10")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permissionId").value(testPermissionId))
                .andReturn().getResponse();

        assertEquals("/permission-status/MyTestId", response.getHeader("Location"));
        verify(mockService).createAndSendPermissionRequest(any());
    }
}