package energy.eddie.regionconnector.us.green.button.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import energy.eddie.regionconnector.us.green.button.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.us.green.button.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestCreationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PermissionRequestController.class)
@Import(PermissionRequestController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestCreationService creationService;
    @SuppressWarnings("unused")
    @MockitoBean
    private PermissionRequestAuthorizationService authorizationService;

    @Test
    void createPermissionRequest_returnsCreatedPermissionRequest() throws Exception {
        // Given
        when(creationService.createPermissionRequest(any()))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("http://localhost")));
        var pr = new PermissionRequestForCreation("cid", "dnid", "http://localhost", "company", "US");

        // When
        mockMvc.perform(
                       post("/permission-request")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(objectMapper.writeValueAsString(pr))
               )
               // Then
               .andExpect(header().exists("Location"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.redirectUri", is("http://localhost")));
    }
}
