package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.services.PermissionRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.UUID;

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
    @Autowired
    private ConfigurableEnvironment environment;
    @MockBean
    private PermissionRequestService service;

    @BeforeEach
    void setUp() {
        environment.setActiveProfiles();
    }

    @Test
    void javascriptConnectorElement_returnsOk() throws Exception {
        // Given

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/dk-energinet/ce.js"))
                // Then
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void javascriptConnectorElement_returnsOk_withDevProfile() throws Exception {
        // Given
        environment.setActiveProfiles("dev");

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/dk-energinet/ce.js"))
                // Then
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void givenNoPermissionId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/region-connectors/dk-energinet/permission-status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNonExistingPermissionId_returnsNotFound() throws Exception {
        String permissionId = "NonExistingId";
        mockMvc.perform(get("/region-connectors/dk-energinet/permission-status/{permissionId}", permissionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("Unable to find permission request")));
    }

    @Test
    void givenExistingPermissionId_returnsConnectionStatusMessage() throws Exception {
        String permissionId = "68916faf-9020-4453-a469-068f076e6d87";
        ConnectionStatusMessage statusMessage = new ConnectionStatusMessage("foo", permissionId,
                "bar", PermissionProcessStatus.ACCEPTED);
        when(service.findConnectionStatusMessageById(permissionId)).thenReturn(Optional.of(statusMessage));

        mockMvc.perform(get("/region-connectors/dk-energinet/permission-status/{permissionId}", permissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.connectionId", is("foo")))
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(jsonPath("$.dataNeedId", is("bar")));
    }

    @Test
    void givenJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foo\": \"bar\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void givenFormUrlEncoded_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void givenNoRequestBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(7),
                        hasItem("dataNeedId must not be blank")
                )));
    }

    @Test
    void givenSomeMissingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("connectionId", "23")
                        .param("meteringPoint", "92345")
                        .param("periodResolution", "PT1H"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(4),
                        hasItem("start must not be null"),
                        hasItem("end must not be null"),
                        hasItem("refreshToken must not be blank"),
                        hasItem("dataNeedId must not be blank")
                )));
    }

    @Test
    void givenInvalidPeriodResolution_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("connectionId", "23")
                        .param("periodResolution", "PT4h"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("Invalid PeriodResolutionEnum")));
    }

    @Test
    void givenBlankFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("connectionId", "")
                        .param("meteringPoint", "92345")
                        .param("periodResolution", "PT1H")
                        .param("refreshToken", "      "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", allOf(
                        iterableWithSize(5),
                        hasItem("start must not be null"),
                        hasItem("end must not be null"),
                        hasItem("dataNeedId must not be blank"),
                        hasItem("refreshToken must not be blank"),
                        hasItem("connectionId must not be blank")
                )));
    }

    @Test
    void givenAdditionalFields_areIgnored() throws Exception {
        var permissionId = UUID.randomUUID().toString();

        when(service.createAndSendPermissionRequest(any())).thenAnswer(invocation -> {
            PermissionRequestForCreation request = invocation.getArgument(0);
            return new EnerginetCustomerPermissionRequest(
                    permissionId, request.connectionId(), request.start(), request.end(),
                    request.refreshToken(), request.meteringPoint(), request.dataNeedId(),
                    request.periodResolution(), mock(EnerginetConfiguration.class)
            );
        });


        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("connectionId", "214")
                        .param("meteringPoint", "92345")
                        .param("periodResolution", "PT1H")
                        .param("refreshToken", "HelloRefreshToken")
                        .param("dataNeedId", "Need")
                        .param("additionalField", "Useless")
                        .param("start", "2023-10-10")
                        .param("end", "2023-12-12"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(header().string("Location", is("/permission-status/" + permissionId)));
    }

    @Test
    void givenValidInput_returnsLocationAndPermissionRequest() throws Exception {
        var permissionId = UUID.randomUUID().toString();

        when(service.createAndSendPermissionRequest(any())).thenAnswer(invocation -> {
            PermissionRequestForCreation request = invocation.getArgument(0);
            return new EnerginetCustomerPermissionRequest(
                    permissionId, request.connectionId(), request.start(), request.end(),
                    request.refreshToken(), request.meteringPoint(), request.dataNeedId(),
                    request.periodResolution(), mock(EnerginetConfiguration.class)
            );
        });

        mockMvc.perform(post("/region-connectors/dk-energinet/permission-request")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("connectionId", "214")
                        .param("meteringPoint", "92345")
                        .param("periodResolution", "PT1H")
                        .param("refreshToken", "HelloRefreshToken")
                        .param("dataNeedId", "Need")
                        .param("additionalField", "Useless")
                        .param("start", "2023-10-10")
                        .param("end", "2023-11-11"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(header().string("Location", is("/permission-status/" + permissionId)));
    }
}