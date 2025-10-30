package energy.eddie.regionconnector.nl.mijn.aansluiting.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
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
    @SuppressWarnings("unused")
    private PrivateKey ignored;
    @MockitoBean
    @SuppressWarnings("unused")
    private PermissionEventRepository ignoredEventRepo;
    @MockitoBean
    @SuppressWarnings("unused")
    private NlPermissionRequestRepository ignoredPermissionRequestRepo;
    @MockitoBean
    private PermissionRequestService service;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    public static Stream<Arguments> testCallback_returnsAnswer_onPermissionStatus() {
        return Stream.of(
                Arguments.of(PermissionProcessStatus.ACCEPTED,
                             "Access granted. You can close this tab now."),
                Arguments.of(PermissionProcessStatus.REJECTED,
                             "Access rejected. You can close this tab now."),
                Arguments.of(PermissionProcessStatus.INVALID,
                             "Invalid answer. Please contact the service provider."),
                Arguments.of(PermissionProcessStatus.UNABLE_TO_SEND,
                             "Permission request could not be sent to permission administrator. Please contact the service provider.")
        );
    }

    public static Stream<Arguments> testCallback_onError_includesErrorMessage() {
        return Stream.of(
                Arguments.of(PermissionProcessStatus.INVALID, """
                        Invalid answer. Please contact the service provider.
                        Answer included an error.
                        error: description"""),
                Arguments.of(PermissionProcessStatus.UNABLE_TO_SEND, """
                        Permission request could not be sent to permission administrator. Please contact the service provider.
                        Answer included an error.
                        error: description""")
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
    void testCreatePermissionRequest_onInvalidBody_returnsBadRequest() throws Exception {
        // Given
        var pr = new PermissionRequestForCreation("cid", "dnid", "12", "asdf");

        // When
        var content = mapper.writeValueAsString(pr);
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               // Then
               .andExpect(status().isBadRequest());
    }

    @Test
    void testCallback_onEmptyQueryString_isInvalid() throws Exception {
        // Given
        // When
        mockMvc.perform(get("/oauth2/code/mijn-aansluiting")
                                .cookie(new Cookie("EDDIE-SESSION-ID", "pid")))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(content().string("Invalid answer. Please contact the service provider."));
    }


    @ParameterizedTest
    @MethodSource
    void testCallback_onError_includesErrorMessage(
            PermissionProcessStatus status,
            String message
    ) throws Exception {
        // Given
        when(service.receiveResponse(any(), any()))
                .thenReturn(status);

        // When
        mockMvc.perform(get("/oauth2/code/mijn-aansluiting")
                                .queryParam("error", "error")
                                .queryParam("error_description", "description")
                                .cookie(new Cookie("EDDIE-SESSION-ID", "pid")))
               // Then
               .andExpect(status().isOk())
               .andExpect(content().string(message));
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

    @Test
    void testCallback_returnsNotImplemented_onUnexpectedPermissionStatus() throws Exception {
        // Given
        when(service.receiveResponse(any(), any()))
                .thenReturn(PermissionProcessStatus.CREATED);

        // When
        mockMvc.perform(get("/oauth2/code/mijn-aansluiting")
                                .queryParam("code", "asdf")
                                .cookie(new Cookie("EDDIE-SESSION-ID", "pid")))
               // Then
               .andExpect(status().isNotImplemented())
               .andExpect(content().string("Response lead to unexpected status: CREATED"));
    }
}
