package energy.eddie.regionconnector.aiida.web;

import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionEventRepository;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.aiida.services.PermissionCreationValidationSendingService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PermissionRequestController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionCreationValidationSendingService service;
    @MockBean
    private AiidaPermissionEventRepository mockRepository;
    @MockBean
    private AiidaPermissionRequestViewRepository mockViewRepository;
    @MockBean
    private DataNeedsService unusedDataNeedsService;

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

    @Test
    void givenNoRequestBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void givenMissingConnectionId_returnsBadRequest() throws Exception {
        var json = "{\"dataNeedId\":\"1\"}";

        mockMvc.perform(post("/permission-request")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("connectionId: must not be blank")));
    }

    @Test
    void givenAdditionalNotNeededInformation_isIgnored() throws Exception {
        // Given
        var permissionId = "SomeId";
        var mockDto = mock(PermissionDto.class);
        when(service.createValidateAndSendPermissionRequest(any())).thenReturn(mockDto);
        when(mockDto.permissionId()).thenReturn(permissionId);
        var requestJson = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"11\",\"extra\":\"information\"}";
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId)
                                                                                            .toString();

        // When
        mockMvc.perform(post("/permission-request")
                                .content(requestJson)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isCreated())
               .andExpect(header().string("Location", is(expectedLocationHeader)))
               .andExpect(jsonPath("$.permissionId", is(permissionId)));
        verify(service).createValidateAndSendPermissionRequest(any());
    }

    @Test
    void givenValidInput_asExpected() throws Exception {
        // Given
        var permissionId = "SecondSomeId";
        var mockDto = mock(PermissionDto.class);
        when(service.createValidateAndSendPermissionRequest(any())).thenReturn(mockDto);
        when(mockDto.permissionId()).thenReturn(permissionId);
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\"}";
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId)
                                                                                            .toString();

        // When
        mockMvc.perform(post("/permission-request")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isCreated())
               .andExpect(header().string("Location", is(expectedLocationHeader)))
               .andExpect(jsonPath("$.permissionId", is(permissionId)));

        verify(service).createValidateAndSendPermissionRequest(any());
    }

    @Test
    void givenUnsupportedDataNeedId_returnsBadRequest() throws Exception {
        // Given
        when(service.createValidateAndSendPermissionRequest(any())).thenThrow(new UnsupportedDataNeedException(
                AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                "test",
                "Is a test reason."));
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"UNSUPPORTED\"}";

        // When
        mockMvc.perform(post("/permission-request")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("Region connector 'aiida' does not support data need with ID 'test': Is a test reason.")));
    }
}
