package energy.eddie.regionconnector.at.eda.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriTemplate;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@Import(PermissionRequestController.class)
class PermissionRequestControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionRequestService permissionRequestService;
    @MockBean
    private PermissionRequestCreationService permissionRequestCreationService;

    private static Stream<Arguments> permissionRequestArguments() {
        return Stream.of(
                Arguments.of("", "0".repeat(33), "dnid", "0".repeat(8), "connectionId"),
                Arguments.of("cid", "", "dnid", "0".repeat(8), "meteringPointId"),
                Arguments.of("cid", "0".repeat(33), "", "0".repeat(8), "dataNeedId"),
                Arguments.of("cid", "0".repeat(33), "dnid", "", "dsoId"),
                Arguments.of(null, "0".repeat(33), "dnid", "0".repeat(8), "connectionId"),
                Arguments.of("cid", "0".repeat(33), null, "0".repeat(8), "dataNeedId")
        );
    }

    @Test
    void permissionStatus_permissionExists_returnsOk() throws Exception {
        // Given
        var state = new AtAcceptedPermissionRequestState(null);
        String permissionId = "permissionId";
        when(permissionRequestService.findConnectionStatusMessageById(permissionId))
                .thenReturn(Optional.of(new ConnectionStatusMessage(
                        "cid",
                        permissionId,
                        "dnid",
                        new EdaDataSourceInformation("dsoId"),
                        state.status(),
                        ""
                )));
        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/permission-status/{permissionId}", permissionId)
                                .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(jsonPath("$.connectionId", is("cid")));
    }

    @Test
    void permissionStatus_permissionDoesNotExist_returnsNotFound() throws Exception {
        // Given
        var state = new AtAcceptedPermissionRequestState(null);
        when(permissionRequestService.findByPermissionId("pid"))
                .thenReturn(Optional.of(new SimplePermissionRequest("pid", "cid", "dnid", "cmId", "conid", state)));
        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/permission-status/{permissionId}", "123")
                                .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No permission with ID '123' found.")));
    }

    @Test
    void createPermissionRequest_415WhenNotJsonBody() throws Exception {
        // Given
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("connectionId", "someValue")
                )
                // Then
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void createPermissionRequest_400WhenNoBody() throws Exception {
        // Given
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void createPermissionRequest_400WhenMissingFields() throws Exception {
        // Given
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(2)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItems(
                        "dataNeedId: must not be blank",
                        "connectionId: must not be blank")));
    }

    @Test
    void createPermissionRequest_400WhenFieldsNotExactSize() throws Exception {
        ObjectNode jsonNode = objectMapper.createObjectNode()
                .put("connectionId", "23")
                .put("dataNeedId", "PT4h")
                .put("dsoId", "123")
                .put("meteringPointId", "456");

        // Given
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(jsonNode))
                )
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(2)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItems(
                        "meteringPointId: needs to be exactly 33 characters long",
                        "dsoId: needs to be exactly 8 characters long")));
    }

    @Test
    void createPermissionRequest_returnsPermissionRequest_andSetsLocationHeader() throws Exception {
        // Given
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand("pid").toString();
        CreatedPermissionRequest expected = new CreatedPermissionRequest("pid", "cmRequestId");
        when(permissionRequestCreationService.createAndSendPermissionRequest(any()))
                .thenReturn(expected);
        var end = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        var start = end.minusDays(1);

        ObjectNode jsonNode = objectMapper.createObjectNode()
                .put("connectionId", "cid")
                .put("meteringPointId", "0".repeat(33))
                .put("dataNeedId", "dnid")
                .put("dsoId", "0".repeat(8))
                .put("start", start.toString())
                .put("end", end.toString())
                .put("granularity", "PT15M");

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonNode.toString())
                )
                // Then
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)))
                .andExpect(header().string("Location", is(expectedLocationHeader)));
    }

    @Test
    void createPermissionRequest_givenUnsupportedGranularity_returnsBadRequest() throws Exception {
        // Given
        CreatedPermissionRequest expected = new CreatedPermissionRequest("pid", "cmRequestId");
        when(permissionRequestCreationService.createAndSendPermissionRequest(any()))
                .thenReturn(expected);
        var end = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        var start = end.minusDays(1);

        ObjectNode jsonNode = objectMapper.createObjectNode()
                .put("connectionId", "cid")
                .put("meteringPointId", "0".repeat(33))
                .put("dataNeedId", "dnid")
                .put("dsoId", "0".repeat(8))
                .put("start", start.toString())
                .put("end", end.toString())
                .put("granularity", "P1M");
        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonNode.toString())
                )
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", startsWith("granularity: Unsupported granularity: 'P1M'.")));
    }

    @Test
    void createPermissionRequest_201WhenEndDateNull() throws Exception {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        ObjectNode jsonNode = objectMapper.createObjectNode()
                .put("connectionId", "cid")
                .put("meteringPointId", "0".repeat(33))
                .put("dataNeedId", "dnid")
                .put("dsoId", "0".repeat(8))
                .put("start", start.toString())
                .putNull("end")
                .put("granularity", "PT15M");

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonNode.toString())
                )
                // Then
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @MethodSource("permissionRequestArguments")
    void createPermissionRequest_400WhenMissingStringParameters(String connectionId, String meteringPoint, String dataNeedsId, String dsoId, String errorFieldName) throws Exception {
        // Given
        var end = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        var start = end.minusDays(1);

        ObjectNode jsonNode = objectMapper.createObjectNode()
                .put("connectionId", connectionId)
                .put("meteringPointId", meteringPoint)
                .put("dataNeedId", dataNeedsId)
                .put("dsoId", dsoId)
                .put("start", start.toString())
                .put("end", end.toString())
                .put("granularity", "PT15M");

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonNode.toString())
                )
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", startsWith(errorFieldName)));
    }

    /**
     * The {@link RegionConnectorsCommonControllerAdvice} is automatically registered for each region connector when the
     * whole core is started. To be able to properly test the controller's error responses, manually add the advice
     * to this test class.
     */
    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
        }
    }
}