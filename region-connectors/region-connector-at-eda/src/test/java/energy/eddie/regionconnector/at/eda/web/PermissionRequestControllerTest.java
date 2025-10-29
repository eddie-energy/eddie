package energy.eddie.regionconnector.at.eda.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationAndValidationService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriTemplate;

import java.util.stream.Stream;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    private PermissionRequestCreationAndValidationService permissionRequestCreationAndValidationService;

    private static Stream<Arguments> permissionRequestArguments() {
        return Stream.of(
                Arguments.of("", "0".repeat(33), "dnid", "0".repeat(8), "connectionId"),
                Arguments.of("cid", "", "dnid", "0".repeat(8), "meteringPointId"),
                Arguments.of("cid", "0".repeat(33), "", "0".repeat(8), "dataNeedId"),
                Arguments.of("cid", "0".repeat(33), "dnid", "A", "dsoId"),
                Arguments.of(null, "0".repeat(33), "dnid", "0".repeat(8), "connectionId"),
                Arguments.of("cid", "0".repeat(33), null, "0".repeat(8), "dataNeedId")
        );
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
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(3)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItems(
                       "dataNeedId: must not be blank",
                       "connectionId: must not be blank",
                       "dsoId: must not be blank")));
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
        var expectedLocationHeader = new UriTemplate(CONNECTION_STATUS_STREAM).expand("pid").toString();
        CreatedPermissionRequest expected = new CreatedPermissionRequest("pid");
        when(permissionRequestCreationAndValidationService.createAndValidatePermissionRequest(any()))
                .thenReturn(expected);

        ObjectNode jsonNode = objectMapper.createObjectNode()
                                          .put("connectionId", "cid")
                                          .put("meteringPointId", "0".repeat(33))
                                          .put("dataNeedId", "dnid")
                                          .put("dsoId", "0".repeat(8))
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
        UnsupportedDataNeedException exception = new UnsupportedDataNeedException(
                EdaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                null,
                "Unsupported granularity: 'JUST_FOR_TEST'");
        when(permissionRequestCreationAndValidationService.createAndValidatePermissionRequest(any()))
                .thenThrow(exception);

        ObjectNode jsonNode = objectMapper.createObjectNode()
                                          .put("connectionId", "cid")
                                          .put("meteringPointId", "0".repeat(33))
                                          .put("dataNeedId", "dnid")
                                          .put("dsoId", "0".repeat(8))
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
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   startsWith(
                                           "Region connector 'at-eda' does not support data need with ID 'null': Unsupported granularity: 'JUST_FOR_TEST'")));
    }

    @ParameterizedTest
    @MethodSource("permissionRequestArguments")
    void createPermissionRequest_400WhenMissingStringParameters(
            String connectionId,
            String meteringPoint,
            String dataNeedsId,
            String dsoId,
            String errorFieldName
    ) throws Exception {
        // Given
        ObjectNode jsonNode = objectMapper.createObjectNode()
                                          .put("connectionId", connectionId)
                                          .put("meteringPointId", meteringPoint)
                                          .put("dataNeedId", dataNeedsId)
                                          .put("dsoId", dsoId)
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
        public DataNeedsAdvice dataNeedsAdvice() {
            return new DataNeedsAdvice();
        }
    }
}
