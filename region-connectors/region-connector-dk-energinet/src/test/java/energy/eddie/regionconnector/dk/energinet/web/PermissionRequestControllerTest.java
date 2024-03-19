package energy.eddie.regionconnector.dk.energinet.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetDataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.persistence.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.services.PermissionCreationService;
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
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
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
    @MockBean
    private PermissionCreationService creationService;
    @MockBean
    private DkEnerginetCustomerPermissionRequestRepository repository;
    @MockBean
    private Flux<IdentifiableApiResponse> unused;
    @Autowired
    private ObjectMapper mapper;

    /**
     * The {@link RegionConnectorsCommonControllerAdvice} is automatically registered for each region connector when the
     * whole core is started. To be able to properly test the controller's error responses, manually add the advice to
     * this test class.
     */
    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
        }

        @Bean
        public DataNeedsAdvice dataNeedsAdvice() {
            return new DataNeedsAdvice();
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
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   startsWith("No permission with ID 'NonExistingId' found.")));
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
                       iterableWithSize(4),
                       hasItem("connectionId: must not be blank"),
                       hasItem("refreshToken: must not be blank"),
                       hasItem("meteringPoint: must not be blank"),
                       hasItem("dataNeedId: must not be blank")
               )));
    }

    @Test
    void givenInvalidGranularity_returnsBadRequest() throws Exception {
        UnsupportedDataNeedException exception = new UnsupportedDataNeedException(
                EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                null,
                "Unsupported granularity: 'JUST_FOR_TEST'");
        when(creationService.createAndSendPermissionRequest(any())).thenThrow(exception);

        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "23")
                                    .put("meteringPoint", "23")
                                    .put("dataNeedId", "23")
                                    .put("refreshToken", "23");

        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jsonNode)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andDo(MockMvcResultHandlers.print())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("Region connector 'dk-energinet' does not support data need with ID 'null': Unsupported granularity: 'JUST_FOR_TEST'")));
    }

    @Test
    void givenBlankFields_returnsBadRequest() throws Exception {
        ObjectNode jsonNode = mapper.createObjectNode()
                                    .put("connectionId", "")
                                    .put("meteringPoint", "92345")
                                    .put("refreshToken", "      ");

        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jsonNode)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", allOf(
                       iterableWithSize(3),
                       hasItem("dataNeedId: must not be blank"),
                       hasItem("refreshToken: must not be blank"),
                       hasItem("connectionId: must not be blank")
               )));
    }

    @Test
    void givenAdditionalFields_areIgnored() throws Exception {
        var permissionId = UUID.randomUUID().toString();
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId)
                                                                                            .toString();

        when(creationService.createAndSendPermissionRequest(any())).thenAnswer(invocation -> {
            PermissionRequestForCreation request = invocation.getArgument(0);
            return new EnerginetCustomerPermissionRequest(
                    permissionId,
                    request,
                    mock(EnerginetCustomerApi.class),
                    LocalDate.now(DK_ZONE_ID),
                    LocalDate.now(DK_ZONE_ID).plusDays(5),
                    Granularity.PT1H,
                    new StateBuilderFactory()
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
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId)
                                                                                            .toString();

        when(creationService.createAndSendPermissionRequest(any())).thenAnswer(invocation -> {
            PermissionRequestForCreation request = invocation.getArgument(0);
            return new EnerginetCustomerPermissionRequest(permissionId,
                                                          request,
                                                          mock(EnerginetCustomerApi.class),
                                                          LocalDate.now(DK_ZONE_ID),
                                                          LocalDate.now(DK_ZONE_ID).plusDays(5),
                                                          Granularity.PT1H,
                                                          new StateBuilderFactory()
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
