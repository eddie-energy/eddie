package energy.eddie.regionconnector.fr.enedis.web;

import energy.eddie.regionconnector.fr.enedis.CimTestConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
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
import org.springframework.web.util.UriTemplate;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
@Import(CimTestConfiguration.class)
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @MockitoBean
    private PermissionRequestService permissionRequestService;

    @Test
    void createPermissionRequest_returnsCreated() throws Exception {
        // Given
        var expectedLocationHeader = new UriTemplate(CONNECTION_STATUS_STREAM).expand("pid").toString();
        when(permissionRequestService.createPermissionRequest(any()))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("https://redirect.com")));
        PermissionRequestForCreation pr = new PermissionRequestForCreation(
                "cid",
                "dnid"
        );

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON_VALUE)
                                             .content(mapper.writeValueAsString(pr))
               )
               // Then
               .andExpect(status().isCreated())
               .andExpect(header().string("Location", is(expectedLocationHeader)));
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
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void givenAllMissingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(2),
                       hasItem("connectionId: must not be blank"),
                       hasItem("dataNeedId: must not be blank")
               )));
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
    }
}
