package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PermissionRequestController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AiidaRegionConnectorService service;

    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
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
    void givenStateTransitionException_returnsInternalServerError() throws Exception {
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\"}";

        when(service.createNewPermission(any())).thenThrow(new PastStateException(mock(PermissionRequestState.class)));

        mockMvc.perform(post("/permission-request")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("An error occurred while trying to transition a permission request to a new state.")));
    }

    @Test
    void givenAdditionalNotNeededInformation_isIgnored() throws Exception {
        // Given
        var permissionId = "SomeId";
        var mockDto = mock(PermissionDto.class);
        when(service.createNewPermission(any())).thenReturn(mockDto);
        when(mockDto.permissionId()).thenReturn(permissionId);
        var requestJson = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"11\",\"extra\":\"information\"}";
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId).toString();

        // When
        mockMvc.perform(post("/permission-request")
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", is(expectedLocationHeader)))
                .andExpect(jsonPath("$.permissionId", is(permissionId)));
        verify(service).createNewPermission(any());
    }

    @Test
    void givenValidInput_asExpected() throws Exception {
        // Given
        var permissionId = "SecondSomeId";
        var mockDto = mock(PermissionDto.class);
        when(service.createNewPermission(any())).thenReturn(mockDto);
        when(mockDto.permissionId()).thenReturn(permissionId);
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\"}";
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId).toString();

        // When
        mockMvc.perform(post("/permission-request")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", is(expectedLocationHeader)))
                .andExpect(jsonPath("$.permissionId", is(permissionId)));

        verify(service).createNewPermission(any());
    }
}