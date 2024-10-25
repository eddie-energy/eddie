package energy.eddie.regionconnector.fi.fingrid.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.fi.fingrid.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fi.fingrid.permission.FingridDataSourceInformation;
import energy.eddie.regionconnector.fi.fingrid.services.PermissionCreationService;
import energy.eddie.regionconnector.fi.fingrid.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class}, properties = {"eddie.jwt.hmac.secret=RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=", "eddie.permission.request.timeout.duration=24"})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionCreationService creationService;
    @MockBean
    private PermissionRequestService requestService;

    @Test
    void connectionStatusMessage_returns() throws Exception {
        // Given
        when(requestService.connectionStatusMessage("pid"))
                .thenReturn(new ConnectionStatusMessage("cid",
                                                        "pid",
                                                        "dnid",
                                                        new FingridDataSourceInformation(),
                                                        PermissionProcessStatus.CREATED));

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/permission-status/{permissionId}", "pid")
                                              .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permissionId", is("pid")));
    }

    @Test
    void connectionStatusMessage_throwsOnUnknownPermissionId() throws Exception {
        // Given
        when(requestService.connectionStatusMessage("pid"))
                .thenThrow(PermissionNotFoundException.class);

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/permission-status/{permissionId}", "pid")
                                              .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNotFound());
    }

    @Test
    void organisationInformation_respondsOk() throws Exception {
        // Given
        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/organisation-information")
                                              .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.organisationUser", is("user-id")))
               .andExpect(jsonPath("$.organisationName", is("org-name")));
    }

    @Test
    void createPermissionRequest_createsPermissionRequest() throws Exception {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid",
                                                                 "0".repeat(36),
                                                                 "identifier",
                                                                 "mid");
        when(creationService.createAndValidatePermissionRequest(permissionRequest))
                .thenReturn(new CreatedPermissionRequest("pid", ""));

        // When
        mockMvc.perform(MockMvcRequestBuilders.post("/permission-request")
                                              .contentType(MediaType.APPLICATION_JSON)
                                              .content(objectMapper.writeValueAsString(permissionRequest)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.permissionId", is("pid")))
               .andExpect(jsonPath("$.accessToken").isString())
               .andExpect(header().string("location", "/permission-status/pid"));
    }

    @Test
    void createPermissionRequest_withInvalidBodyReturnsInvalid() throws Exception {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid",
                                                                 "0".repeat(15),
                                                                 "identifier",
                                                                 "mid");

        // When
        mockMvc.perform(MockMvcRequestBuilders.post("/permission-request")
                                              .contentType(MediaType.APPLICATION_JSON)
                                              .content(objectMapper.writeValueAsString(permissionRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createPermissionRequest_withUnknownDataNeedId() throws Exception {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid",
                                                                 "0".repeat(36),
                                                                 "identifier",
                                                                 "mid");
        when(creationService.createAndValidatePermissionRequest(permissionRequest))
                .thenThrow(DataNeedNotFoundException.class);

        // When
        mockMvc.perform(MockMvcRequestBuilders.post("/permission-request")
                                              .contentType(MediaType.APPLICATION_JSON)
                                              .content(objectMapper.writeValueAsString(permissionRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createPermissionRequest_withUnsupportedDataNeed() throws Exception {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid",
                                                                 "0".repeat(36),
                                                                 "identifier",
                                                                 "mid");
        when(creationService.createAndValidatePermissionRequest(permissionRequest))
                .thenThrow(UnsupportedDataNeedException.class);

        // When
        mockMvc.perform(MockMvcRequestBuilders.post("/permission-request")
                                              .contentType(MediaType.APPLICATION_JSON)
                                              .content(objectMapper.writeValueAsString(permissionRequest)))
               .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"accepted", "rejected"})
    void acceptPermissionRequest_returnsOk(String path) throws Exception {
        // Given
        // When
        mockMvc.perform(MockMvcRequestBuilders.patch("/permission-request/{permissionId}/{path}", "pid", path))
               // Then
               .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"accepted", "rejected"})
    void acceptPermissionRequest_returnsNotFoundOnUnknownPermissionRequest(String path) throws Exception {
        // Given
        doThrow(PermissionNotFoundException.class).when(creationService).acceptOrReject(eq("pid"), any());
        // When
        mockMvc.perform(MockMvcRequestBuilders.patch("/permission-request/{permissionId}/{path}", "pid", path))
               // Then
               .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"accepted", "rejected"})
    void acceptPermissionRequest_isBadRequestOnWrongPermissionStatus(String path) throws Exception {
        // Given
        doThrow(PermissionStateTransitionException.class).when(creationService).acceptOrReject(eq("pid"), any());
        // When
        mockMvc.perform(MockMvcRequestBuilders.patch("/permission-request/{permissionId}/{path}", "pid", path))
               // Then
               .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
        }

        @Bean
        public JwtUtil jwtUtil(
                @Value("${eddie.jwt.hmac.secret}") String jwtHmacSecret,
                @Value("${eddie.permission.request.timeout.duration}") int timeoutDuration
        ) {
            return new JwtUtil(jwtHmacSecret, timeoutDuration);
        }
    }
}
