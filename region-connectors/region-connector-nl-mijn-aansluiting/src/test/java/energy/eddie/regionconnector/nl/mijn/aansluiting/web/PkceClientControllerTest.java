package energy.eddie.regionconnector.nl.mijn.aansluiting.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.MijnAansluitingDataSourceInformation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PermissionRequestService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.security.PrivateKey;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
class PkceClientControllerTest {
    @MockitoBean
    private PrivateKey ignored;
    @MockitoBean
    private PermissionEventRepository ignoredEventRepo;
    @MockitoBean
    private NlPermissionRequestRepository ignoredPermissionRequestRepo;
    @MockitoBean
    private PermissionRequestService service;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    public static Stream<Arguments> testCallback_returnsAnswer_onPermissionStatus() {
        return Stream.of(
                Arguments.of(PermissionProcessStatus.ACCEPTED, "Access Granted. You can close this tab now."),
                Arguments.of(PermissionProcessStatus.REJECTED,
                             "Permission Request rejected. You can close this tab now."),
                Arguments.of(PermissionProcessStatus.INVALID, "Unable to complete request. You can close this tab now.")
        );
    }

    @Test
    void testCreatePermissionRequest_returnsPermissionRequest() throws Exception {
        // Given
        when(service.createPermissionRequest(any()))
                .thenReturn(new CreatedPermissionRequest("pid", URI.create("localhost")));
        var pr = new PermissionRequestForCreation("cid", "dnid", "12");

        // When
        var content = mapper.writeValueAsString(pr);
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               // Then
               .andExpect(cookie().exists("EDDIE-SESSION-ID"))
               .andExpect(header().exists("Location"))
               .andExpect(content().json("{\"permissionId\":  \"pid\", \"redirectUri\":  \"localhost\"}"));
    }

    @Test
    void testPermissionRequestStatus_returnsPermissionRequestStatus() throws Exception {
        // Given
        var timestamp = ZonedDateTime.of(2024, 4, 18, 0, 0, 0, 0, ZoneOffset.UTC);
        when(service.connectionStatusMessage(any()))
                .thenReturn(new ConnectionStatusMessage("cid",
                                                        "pid",
                                                        "dnid",
                                                        new MijnAansluitingDataSourceInformation(),
                                                        timestamp,
                                                        PermissionProcessStatus.ACCEPTED,
                                                        "",
                                                        null));

        // When
        mockMvc.perform(get("/permission-status/pid"))
               // Then
               .andExpect(content().json(
                       """
                               {
                                   "connectionId":  "cid",
                                   "permissionId":  "pid",
                                   "dataNeedId":  "dnid",
                                   "dataSourceInformation": {
                                        "countryCode": "NL",
                                        "meteredDataAdministratorId":"Miijn Aansluiting",
                                        "permissionAdministratorId":"Miijn Aansluiting",
                                        "regionConnectorId":"nl-mijn-aansluiting"
                                   },
                                   "timestamp": "2024-04-18T00:00:00Z",
                                   "status":  "ACCEPTED",
                                   "message": ""
                               }
                               """));
    }

    @Test
    void testCallback_onEmptyQueryString_isInvalid() throws Exception {
        // Given
        when(service.receiveResponse(any(), any()))
                .thenReturn(PermissionProcessStatus.ACCEPTED);

        // When
        mockMvc.perform(get("/oauth2/code/mijn-aansluiting")
                                .cookie(new Cookie("EDDIE-SESSION-ID", "pid")))
               // Then
               .andExpect(status().isOk())
               .andExpect(content().string("Invalid Answer. You can close this tab now."));
    }

    @ParameterizedTest
    @MethodSource
    void testCallback_returnsAnswer_onPermissionStatus(
            PermissionProcessStatus status,
            String message
    ) throws Exception {
        // Given
        when(service.receiveResponse(any(), any()))
                .thenReturn(status);

        // When
        mockMvc.perform(get("/oauth2/code/mijn-aansluiting")
                                .queryParam("code", "asdf")
                                .cookie(new Cookie("EDDIE-SESSION-ID", "pid")))
               // Then
               .andExpect(status().isOk())
               .andExpect(content().string(message));
    }
}
