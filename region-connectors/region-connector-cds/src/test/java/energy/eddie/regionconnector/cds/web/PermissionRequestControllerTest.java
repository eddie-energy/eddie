package energy.eddie.regionconnector.cds.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.cds.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.permission.requests.CdsDataSourceInformation;
import energy.eddie.regionconnector.cds.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.cds.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.web.RestApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import java.net.URI;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@Import({PermissionRequestController.class, PermissionRequestControllerAdvice.class})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestCreationService creationService;
    @MockitoBean
    private PermissionRequestService permissionRequestService;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws Exception {
        // Given
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand("pid")
                .toString();
        var pr = new PermissionRequestForCreation(1L, "dnid", "cid");
        when(creationService.createPermissionRequest(pr))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("urn:example")));


        // When
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pr)))
               // Then
               .andExpect(header().string("Location", is(expectedLocationHeader)))
               .andExpect(content().json("{\"permissionId\":  \"pid\", \"redirectUri\": \"urn:example\"}"));
    }

    @Test
    void testCreatePermissionRequest_withUnknownCdsServer_returnsBadRequest() throws Exception {
        // Given
        var pr = new PermissionRequestForCreation(1L, "dnid", "cid");
        when(creationService.createPermissionRequest(pr))
                .thenThrow(new UnknownPermissionAdministratorException(1L));

        // When
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pr)))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].message", equalTo("Unknown permission administrator: 1")));
    }

    @Test
    void testPermissionUpdate_returnsConnectionStatusMessage() throws Exception {
        // Given
        when(permissionRequestService.getConnectionStatusMessage("pid"))
                .thenReturn(new ConnectionStatusMessage("cid",
                                                        "pid",
                                                        "dnid",
                                                        new CdsDataSourceInformation(1),
                                                        PermissionProcessStatus.ACCEPTED));

        // When
        mockMvc.perform(get(RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM, "pid"))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permissionId").value("pid"));
    }
}