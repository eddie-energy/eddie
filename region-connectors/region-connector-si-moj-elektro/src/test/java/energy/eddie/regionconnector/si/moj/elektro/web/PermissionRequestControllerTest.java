package energy.eddie.regionconnector.si.moj.elektro.web;

import energy.eddie.regionconnector.si.moj.elektro.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.si.moj.elektro.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.si.moj.elektro.service.PermissionRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriTemplate;
import tools.jackson.databind.ObjectMapper;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@AutoConfigureMockMvc(addFilters = false) // disables spring security filters
class PermissionRequestControllerTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestService service;

    @Test
    void testCreatePermissionRequest_returnsCreated() throws Exception {
        // Given
        var expectedLocationHeader = new UriTemplate(CONNECTION_STATUS_STREAM).expand("pid").toString();
        var createdPr = new CreatedPermissionRequest("pid");
        when(service.createPermissionRequest(any())).thenReturn(createdPr);
        var prForCreation = new PermissionRequestForCreation(
                "cid",
                "dnid",
                "apitoken"
        );

        // When
        mockMvc.perform(
                MockMvcRequestBuilders.post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prForCreation))
                )
        // Then
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(createdPr)))
                .andExpect(header().string("Location", is(expectedLocationHeader)));
    }
}
