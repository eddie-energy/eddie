package energy.eddie.regionconnector.si.moj.elektro.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.si.moj.elektro.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.si.moj.elektro.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.si.moj.elektro.permission.MojElektroDataSourceInformation;
import energy.eddie.regionconnector.si.moj.elektro.service.PermissionRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand("pid").toString();
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

    @Test
    void testPermissionStatus_returnsStatus() throws Exception {
        // Given
        var csm = new ConnectionStatusMessage(
                "cid",
                "pid",
                "dnid",
                new MojElektroDataSourceInformation(),
                PermissionProcessStatus.ACCEPTED
        );

        when(service.findConnectionStatusMessageById("pid"))
                .thenReturn(Optional.of(csm));

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get(PATH_PERMISSION_STATUS_WITH_PATH_PARAM,"pid")
                                .contentType(MediaType.APPLICATION_JSON)
                )
        // Then
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(csm)));
    }
}
