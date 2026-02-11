// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationAndValidationService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
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
                       "dataNeedIds: must not be empty",
                       "connectionId: must not be blank",
                       "dsoId: must not be blank")));
    }

    @Test
    void createPermissionRequest_400WhenFieldsNotExactSize() throws Exception {
        var dto = new PermissionRequestForCreation(
                "cid",
                "123",
                List.of("dnid"),
                "456"
        );

        // Given
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(objectMapper.writeValueAsString(dto))
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
        CreatedPermissionRequest expected = new CreatedPermissionRequest(List.of("pid"));
        when(permissionRequestCreationAndValidationService.createAndValidatePermissionRequest(any()))
                .thenReturn(expected);

        var dto = new PermissionRequestForCreation(
                "cid",
                "0".repeat(33),
                List.of("dnid"),
                "0".repeat(8)
        );

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(objectMapper.writeValueAsString(dto))
               )
               // Then
               .andExpect(status().isCreated())
               .andExpect(content().json(objectMapper.writeValueAsString(expected)))
               .andExpect(header().string("Location", is("/api/connection-status-messages?permission-id=pid")));
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

        var dto = new PermissionRequestForCreation(
                "cid",
                "0".repeat(33),
                List.of("dnid"),
                "0".repeat(8)
        );

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(objectMapper.writeValueAsString(dto))
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
            List<String> dataNeedsId,
            String dsoId,
            String errorFieldName
    ) throws Exception {
        // Given
        var dto = new PermissionRequestForCreation(
                connectionId,
                meteringPoint,
                dataNeedsId,
                dsoId
        );

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(objectMapper.writeValueAsString(dto))
               )
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", startsWith(errorFieldName)));
    }

    @Test
    void createPermissionRequest_returnsBadRequest_ifNoPermissionWasCreated() throws Exception {
        // Given
        CreatedPermissionRequest expected = new CreatedPermissionRequest(List.of());
        when(permissionRequestCreationAndValidationService.createAndValidatePermissionRequest(any()))
                .thenReturn(expected);

        var dto = new PermissionRequestForCreation(
                "cid",
                "0".repeat(33),
                List.of("dnid"),
                "0".repeat(8)
        );

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(objectMapper.writeValueAsString(dto))
               )
               // Then
               .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> permissionRequestArguments() {
        return Stream.of(
                Arguments.of("", "0".repeat(33), List.of("dnid"), "0".repeat(8), "connectionId"),
                Arguments.of("cid", "", List.of("dnid"), "0".repeat(8), "meteringPointId"),
                Arguments.of("cid", "0".repeat(33), List.of(), "0".repeat(8), "dataNeedId"),
                Arguments.of("cid", "0".repeat(33), List.of("dnid"), "A", "dsoId"),
                Arguments.of(null, "0".repeat(33), List.of("dnid"), "0".repeat(8), "connectionId"),
                Arguments.of("cid", "0".repeat(33), null, "0".repeat(8), "dataNeedId")
        );
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
