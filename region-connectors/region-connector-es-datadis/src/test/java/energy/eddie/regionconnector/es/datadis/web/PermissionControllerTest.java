package energy.eddie.regionconnector.es.datadis.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisDataSourceInformation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PermissionController.class})
class PermissionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionRequestService mockService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void permissionStatus_permissionExists_returnsOk() throws Exception {
        // Given
        var state = new AcceptedState(null);
        var datadisDataSourceInformation = new DatadisDataSourceInformation(mock(EsPermissionRequest.class));
        String permissionId = "ValidId";
        var statusMessage = new ConnectionStatusMessage("cid", permissionId, "dnid", datadisDataSourceInformation, state.status());
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
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("No permission with ID 123 found")
                )));
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
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("No permission with ID 123 found"))));
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
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("No permission with ID 123 found"))));
    }

    @Test
    void requestPermission_missingRequiredFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"connectionId\": \"Hello World\"}"))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(6),
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
        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "foo")
                .put("meteringPointId", "bar")
                .put("dataNeedId", "du")
                .put("nif", "muh")
                .put("requestDataFrom", "2023-09-09")
                .put("requestDataTo", "2023-10-10");

        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(jsonNode)))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("measurementType must not be null")
                )));
    }

    @Test
    void requestPermission_invalidMeasurementType_returnsBadRequest() throws Exception {
        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "foo")
                .put("meteringPointId", "bar")
                .put("dataNeedId", "du")
                .put("nif", "muh")
                .put("requestDataFrom", "2023-09-09")
                .put("requestDataTo", "2023-10-10")
                .put("measurementType", "INVALID");


        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(jsonNode))
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem(org.hamcrest.Matchers.startsWith("Invalid enum value: 'INVALID' for the field 'measurementType'. The value must ")))));
    }

    @Test
    void requestPermission_noBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(1),
                        hasItem("Invalid request body"))));
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
                .put("nif", "")
                .put("measurementType", "QUARTER_HOURLY");

        mockMvc.perform(post("/permission-request")
                        .content(mapper.writeValueAsString(jsonNode))
                        .contentType(MediaType.APPLICATION_JSON)
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


        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "ConnId")
                .put("meteringPointId", "SomeId")
                .put("dataNeedId", "BLA_BLU_BLE")
                .put("nif", "NOICE")
                .put("measurementType", "QUARTER_HOURLY")
                .put("requestDataFrom", "2023-09-09")
                .put("requestDataTo", "2023-10-10");

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
}