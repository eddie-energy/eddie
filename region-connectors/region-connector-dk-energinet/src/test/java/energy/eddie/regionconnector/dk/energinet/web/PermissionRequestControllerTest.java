package energy.eddie.regionconnector.dk.energinet.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetDataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.services.PermissionRequestService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.util.UriTemplate;

import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionRequestService service;
    @Autowired
    private ObjectMapper mapper;

    /**
     * The {@link RegionConnectorsCommonControllerAdvice} is automatically registered for each region connector when the
     * whole core is started. To be able to properly test the controller's error responses, manually add the advice
     * to this test class.
     */
    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
        }
    }

    @Test
    void givenNoPermissionId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/permission-status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNonExistingPermissionId_returnsNotFound() throws Exception {
        String permissionId = "NonExistingId";
        mockMvc.perform(get("/permission-status/{permissionId}", permissionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", startsWith("No permission with ID 'NonExistingId' found.")));
    }

    @Test
    void givenExistingPermissionId_returnsConnectionStatusMessage() throws Exception {
        String permissionId = "68916faf-9020-4453-a469-068f076e6d87";
        ConnectionStatusMessage statusMessage = new ConnectionStatusMessage(
                "foo",
                permissionId,
                "bar",
                new EnerginetDataSourceInformation(),
                PermissionProcessStatus.ACCEPTED);
        when(service.findConnectionStatusMessageById(permissionId)).thenReturn(Optional.of(statusMessage));

        mockMvc.perform(get("/permission-status/{permissionId}", permissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.connectionId", is("foo")))
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(jsonPath("$.dataNeedId", is("bar")));
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
                        iterableWithSize(7),
                        hasItem("connectionId: must not be blank"),
                        hasItem("start: must not be null"),
                        hasItem("end: must not be null"),
                        hasItem("refreshToken: must not be blank"),
                        hasItem("granularity: must not be null"),
                        hasItem("meteringPoint: must not be blank"),
                        hasItem("dataNeedId: must not be blank")
                )));
    }

    @Test
    void givenInvalidGranularity_returnsBadRequest() throws Exception {
        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "23")
                .put("granularity", "PT4h");

        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(jsonNode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", startsWith("granularity: Invalid enum value: 'PT4h'. Valid values: [")));
    }

    @Test
    void givenBlankFields_returnsBadRequest() throws Exception {
        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "")
                .put("meteringPoint", "92345")
                .put("granularity", "PT1H")
                .put("refreshToken", "      ");

        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(jsonNode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                        iterableWithSize(5),
                        hasItem("start: must not be null"),
                        hasItem("end: must not be null"),
                        hasItem("dataNeedId: must not be blank"),
                        hasItem("refreshToken: must not be blank"),
                        hasItem("connectionId: must not be blank")
                )));
    }

    @Test
    void givenUnsupportedGranularity_returnsBadRequest() throws Exception {
        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "214")
                .put("meteringPoint", "92345")
                .put("granularity", "PT5M")
                .put("refreshToken", "HelloRefreshToken")
                .put("dataNeedId", "Need")
                .put("start", "2023-10-10")
                .put("end", "2023-12-12");

        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(jsonNode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", startsWith("granularity: Unsupported granularity: 'PT5M'.")));
    }


    @Test
    void givenAdditionalFields_areIgnored() throws Exception {
        var permissionId = UUID.randomUUID().toString();
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId).toString();

        when(service.createAndSendPermissionRequest(any())).thenAnswer(invocation -> {
            PermissionRequestForCreation request = invocation.getArgument(0);
            return new EnerginetCustomerPermissionRequest(
                    permissionId, request, mock(EnerginetCustomerApi.class)
            );
        });

        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "214")
                .put("meteringPoint", "92345")
                .put("granularity", "PT1H")
                .put("refreshToken", "HelloRefreshToken")
                .put("dataNeedId", "Need")
                .put("additionalField", "Useless")
                .put("start", "2023-10-10")
                .put("end", "2023-12-12");

        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(jsonNode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(header().string("Location", is(expectedLocationHeader)));
    }

    @Test
    void givenValidInput_returnsLocationAndPermissionRequestId() throws Exception {
        var permissionId = UUID.randomUUID().toString();
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId).toString();

        when(service.createAndSendPermissionRequest(any())).thenAnswer(invocation -> {
            PermissionRequestForCreation request = invocation.getArgument(0);
            return new EnerginetCustomerPermissionRequest(
                    permissionId, request, mock(EnerginetCustomerApi.class)
            );
        });

        ObjectNode jsonNode = mapper.createObjectNode()
                .put("connectionId", "214")
                .put("meteringPoint", "92345")
                .put("granularity", "PT1H")
                .put("refreshToken", "HelloRefreshToken")
                .put("dataNeedId", "Need")
                .put("additionalField", "Useless")
                .put("start", "2023-10-10")
                .put("end", "2023-11-11");

        mockMvc.perform(post("/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(jsonNode)))
                .andExpect(status().isCreated())
                .andExpect(header().string("content-type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(header().string("Location", is(expectedLocationHeader)));
    }
}
