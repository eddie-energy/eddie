package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.services.InvalidRefreshTokenException;
import energy.eddie.regionconnector.dk.energinet.services.PermissionCreationService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.util.UriTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
@Import({PermissionRequestControllerAdvice.class})
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionCreationService creationService;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void givenNoPermissionId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/permission-status"))
               .andExpect(status().isNotFound());
    }

    @Test
    void givenNonJsonBody_returnsUnsupportedMediaType() throws Exception {
        // Given
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.MULTIPART_FORM_DATA)
                                             .param("connectionId", "someValue")
               )
               // Then
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void givenNoRequestBody_returnsBadRequest() throws Exception {
        mockMvc.perform(
                       post("/permission-request")
                               .contentType(MediaType.APPLICATION_JSON)
                               .accept(MediaType.APPLICATION_JSON)
               )
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void givenAllMissingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(
                       post("/permission-request")
                               .contentType(MediaType.APPLICATION_JSON)
                               .accept(MediaType.APPLICATION_JSON)
                               .content("{}")
               )
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(4),
                       hasItem("connectionId: must not be blank"),
                       hasItem("refreshToken: must not be blank"),
                       hasItem("meteringPoint: must not be blank"),
                       hasItem("dataNeedId: must not be blank")
               )));
    }

    @Test
    void givenInvalidGranularity_returnsBadRequest() throws Exception {
        UnsupportedDataNeedException exception = new UnsupportedDataNeedException(
                EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                null,
                "Unsupported granularity: 'JUST_FOR_TEST'");
        when(creationService.createPermissionRequest(any())).thenThrow(exception);

        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "23")
                                    .put("meteringPoint", "23")
                                    .put("dataNeedId", "23")
                                    .put("refreshToken", "23");

        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jsonNode)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andDo(MockMvcResultHandlers.print())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("Region connector 'dk-energinet' does not support data need with ID 'null': Unsupported granularity: 'JUST_FOR_TEST'")));
    }

    @Test
    void givenBlankFields_returnsBadRequest() throws Exception {
        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "")
                                    .put("meteringPoint", "92345")
                                    .put("refreshToken", "      ");

        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jsonNode)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(3),
                       hasItem("dataNeedId: must not be blank"),
                       hasItem("refreshToken: must not be blank"),
                       hasItem("connectionId: must not be blank")
               )));
    }

    @Test
    void givenAdditionalFields_areIgnored() throws Exception {
        var permissionId = UUID.randomUUID().toString();
        var expectedLocationHeader = new UriTemplate(CONNECTION_STATUS_STREAM).expand(permissionId).toString();

        when(creationService.createPermissionRequest(any())).thenReturn(new CreatedPermissionRequest(permissionId));

        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "214")
                                    .put("meteringPoint", "92345")
                                    .put("granularity", "PT1H")
                                    .put("refreshToken", "HelloRefreshToken")
                                    .put("dataNeedId", "Need")
                                    .put("additionalField", "Useless")
                                    .put("start", "2023-10-10")
                                    .put("end", "2023-12-12");

        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jsonNode)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.permissionId", is(permissionId)))
               .andExpect(header().string("Location", is(expectedLocationHeader)));
    }

    @Test
    void givenValidInput_returnsLocationAndPermissionRequestId() throws Exception {
        var permissionId = UUID.randomUUID().toString();
        var expectedLocationHeader = new UriTemplate(CONNECTION_STATUS_STREAM).expand(permissionId).toString();

        when(creationService.createPermissionRequest(any())).thenReturn(new CreatedPermissionRequest(permissionId));

        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "214")
                                    .put("meteringPoint", "92345")
                                    .put("granularity", "PT1H")
                                    .put("refreshToken", "HelloRefreshToken")
                                    .put("dataNeedId", "Need")
                                    .put("additionalField", "Useless")
                                    .put("start", "2023-10-10")
                                    .put("end", "2023-11-11");

        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jsonNode)))
               .andExpect(status().isCreated())
               .andExpect(header().string("content-type", MediaType.APPLICATION_JSON_VALUE))
               .andExpect(jsonPath("$.permissionId", is(permissionId)))
               .andExpect(header().string("Location", is(expectedLocationHeader)));
    }

    @Test
    void givenInvalidRefreshToken_returnsBadRequest() throws Exception {
        when(creationService.createPermissionRequest(any()))
                .thenThrow(new InvalidRefreshTokenException());

        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "23")
                                    .put("meteringPoint", "23")
                                    .put("dataNeedId", "23")
                                    .put("refreshToken", "23");

        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jsonNode)))
               .andExpect(status().isBadRequest());
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

        @Bean
        public CommonInformationModelConfiguration commonInformationModelConfiguration() {
            return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                                                "EP-ID");
        }
    }
}
