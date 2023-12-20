package energy.eddie.regionconnector.fr.enedis.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private ServletWebServerApplicationContext unused;
    @MockBean
    private PermissionRequestService permissionRequestService;

    @Test
    void javascriptConnectorElement_returnsOk() throws Exception {
        // Given
        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/fr-enedis/ce.js"))
                // Then
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void permissionStatus_permissionExists_returnsOk() throws Exception {
        // Given
        when(permissionRequestService.findConnectionStatusMessageById(anyString()))
                .thenReturn(Optional.of(new ConnectionStatusMessage("cid", "permissionId", "dnid", null, PermissionProcessStatus.CREATED)));
        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/region-connectors/fr-enedis/permission-status/" + "cid")
                                .accept(MediaType.APPLICATION_JSON)
                )
                // Then
                .andExpect(status().isOk());
    }

    @Test
    void permissionStatus_permissionDoesNotExists_returnsNotFound() throws Exception {
        // Given
        when(permissionRequestService.findConnectionStatusMessageById(anyString()))
                .thenReturn(Optional.empty());
        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/region-connectors/fr-enedis/permission-status/" + "cid")
                                .accept(MediaType.APPLICATION_JSON)
                )
                // Then
                .andExpect(status().isNotFound());
    }

    @Test
    void createPermissionRequestWithJSONBody_returnsCreated() throws Exception {
        // Given
        when(permissionRequestService.createPermissionRequest(any()))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("https://redirect.com")));
        PermissionRequestForCreation pr = new PermissionRequestForCreation(
                "cid",
                "dnid",
                ZonedDateTime.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(10)
        );

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/region-connectors/fr-enedis/permission-request")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(mapper.writeValueAsString(pr))
                )
                // Then
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void createPermissionRequestUrlEncoded_returnsCreated() throws Exception {
        // Given
        when(permissionRequestService.createPermissionRequest(any()))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("https://redirect.com")));
        PermissionRequestForCreation pr = new PermissionRequestForCreation(
                "cid",
                "dnid",
                ZonedDateTime.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(10)
        );

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/region-connectors/fr-enedis/permission-request")
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                .param("connectionId", pr.connectionId())
                                .param("dataNeedId", pr.dataNeedId())
                                .param("start", pr.start().toLocalDate().toString())
                                .param("end", pr.end().toLocalDate().toString())
                )
                // Then
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void callback_returnsOk() throws Exception {
        // Given
        doNothing().when(permissionRequestService).authorizePermissionRequest(anyString(), anyString());

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/region-connectors/fr-enedis/authorization-callback")
                                .param("state", "state")
                                .param("usage_point_id", "upid")
                )
                // Then
                .andExpect(status().isOk());
    }

}