package energy.eddie.regionconnector.be.fluvius.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusDataSourceInformation;
import energy.eddie.regionconnector.be.fluvius.service.PermissionRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriTemplate;

import java.util.Optional;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static org.hamcrest.Matchers.is;
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
    @MockBean
    private PermissionRequestService service;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws Exception {
        // Given
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand("pid").toString();
        var created = new CreatedPermissionRequest("pid");
        when(service.createPermissionRequest(any())).thenReturn(created);
        var jsonNode = objectMapper.createObjectNode()
                                   .put("connectionId", "cid")
                                   .put("dataNeedId", "dnid")
                                   .put("flow", "B2B");

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.post("/permission-request")
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(jsonNode.toString())
               )
               // Then
               .andExpect(status().isCreated())
               .andExpect(content().json(objectMapper.writeValueAsString(created)))
               .andExpect(header().string("Location", is(expectedLocationHeader)));
    }

    @Test
    void testPermissionStatus_returnsStatus() throws Exception {
        // Given
        var value = new ConnectionStatusMessage(
                "cid", "pid", "dnid", new FluviusDataSourceInformation(), PermissionProcessStatus.ACCEPTED
        );
        when(service.findConnectionStatusMessageById("pid"))
                .thenReturn(Optional.of(
                        value
                ));

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get(PATH_PERMISSION_STATUS_WITH_PATH_PARAM, "pid")
                                             .contentType(MediaType.APPLICATION_JSON)
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(content().json(objectMapper.writeValueAsString(value)));
    }
}