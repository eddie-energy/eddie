// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.dtos.permission.PermissionDto;
import energy.eddie.aiida.errors.GlobalExceptionHandler;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.IncompatibleDataSourceException;
import energy.eddie.aiida.errors.permission.*;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.aiida.services.ConnectionStatusService;
import energy.eddie.aiida.services.PermissionService;
import energy.eddie.aiida.streamers.ConnectionStatusEventStream;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PermissionController.class)
@MockitoBean(types = AuthService.class)
@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {
    private static final String OUTBOUND_ACCEPT_BODY = """
            {"eddieId":"%s","handshakeUrl":"http://localhost/permission-request/{permissionId}","accessToken":"token","dataSourceId":"%s"}""";
    private static final String INBOUND_ACCEPT_BODY = """
            {"eddieId":"%s","handshakeUrl":"http://localhost/permission-request/{permissionId}","accessToken":"token"}""";
    private final UUID eddieId = UUID.fromString("31d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID permissionId = UUID.fromString("41d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID dataSourceId = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID userId = UUID.randomUUID();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private StreamerManager streamerManager;
    @Mock
    private Permission mockPermission;
    private Instant grant;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        grant = Instant.now();
        var permission = new Permission(eddieId, permissionId, "https://example.org", "accessToken", userId);
        when(permissionService.acceptOutboundPermission(any(), any())).thenReturn(permission);
        when(permissionService.acceptInboundPermission(any(), any())).thenReturn(permission);
        when(permissionService.revokePermission(any())).thenReturn(permission);
        when(permissionService.updateInboundMessageFormat(any(), any())).thenReturn(permission);
        when(permissionService.updateDisplayName(any(), any())).thenReturn(permission);

        mapper = ObjectMapperCreatorUtil.mapper();
    }

    @Test
    @WithMockUser
    void givenInvalidAcceptMediaType_returnsNotAcceptable() throws Exception {
        mockMvc.perform(get("/permissions").accept(MediaType.APPLICATION_XML)).andExpect(status().isNotAcceptable());
    }

    @Test
    @WithMockUser
    void getAllPermissionsByGrantDate_returnsEmptyList() throws Exception {
        List<Permission> empty = Collections.emptyList();
        when(permissionService.getAllPermissionsSortedByGrantTime()).thenReturn(empty);

        var responseString = mockMvc.perform(get("/permissions"))
                                    .andExpect(status().isOk())
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString();

        var response = mapper.readValue(responseString, new TypeReference<List<PermissionDto>>() {});
        assertEquals(0, response.size());
    }

    @Test
    @WithMockUser
    void getAllPermissions_includesConnectionLimitFields() throws Exception {
        when(mockPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        when(mockPermission.monitoringDataSourceId()).thenReturn(dataSourceId);
        when(mockPermission.supportsConnectionLimits()).thenReturn(true);
        when(permissionService.getAllPermissionsSortedByGrantTime()).thenReturn(List.of(mockPermission));

        mockMvc.perform(get("/permissions"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].monitoringDataSourceId").value(dataSourceId.toString()))
               .andExpect(jsonPath("$[0].supportsConnectionLimits").value(true));
    }

    @Test
    @WithMockUser
    void getAllPermissions_includesDerivedConnectionStatusForAcceptedPermissions() throws Exception {
        var accepted = new Permission(eddieId, permissionId, "https://example.org", "accessToken", userId);
        accepted.setStatus(PermissionStatus.ACCEPTED);
        accepted.setStartTime(Instant.now().minusSeconds(60));
        var revoked = new Permission(eddieId, UUID.randomUUID(), "https://example.org", "accessToken", userId);
        revoked.setStatus(PermissionStatus.REVOKED);
        when(permissionService.getAllPermissionsSortedByGrantTime()).thenReturn(List.of(accepted, revoked));
        when(streamerManager.isConnected(permissionId)).thenReturn(true);

        mockMvc.perform(get("/permissions"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
               .andExpect(jsonPath("$[0].connectionStatus").value("CONNECTED"))
               .andExpect(jsonPath("$[1].status").value("REVOKED"))
               .andExpect(jsonPath("$[1].connectionStatus").doesNotExist());
    }

    @Test
    @WithMockUser
    void getAllPermissionsByGrantDate_returnsCorrectOrder() throws Exception {
        var permissions = sampleDataForGetAllPermissionsTest();
        when(permissionService.getAllPermissionsSortedByGrantTime()).thenReturn(permissions);

        mockMvc.perform(get("/permissions"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(3))
               .andExpect(jsonPath("$[0].permissionId").value(permissionId.toString()))
               .andExpect(jsonPath("$[0].eddieId").value(eddieId.toString()))
               .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
               .andExpect(jsonPath("$[0].handshakeUrl").doesNotExist())
               .andExpect(jsonPath("$[0].accessToken").doesNotExist());
    }

    @Test
    @WithMockUser
    void getAcceptedInboundPermissions_returnsPermissionsWithDataSource() throws Exception {
        when(mockPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        when(permissionService.getAcceptedInboundPermissions()).thenReturn(List.of(mockPermission));

        mockMvc.perform(get("/permissions/inbound/active"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1));

        verify(permissionService).getAcceptedInboundPermissions();
    }

    @Test
    @WithMockUser
    void givenInvalidMediaType_permissionRequest_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/permissions/details").with(csrf()).contentType(MediaType.APPLICATION_CBOR))
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser
    void givenInvalidAcceptMediaType_permissionRequest_returnsNotAcceptable() throws Exception {
        mockMvc.perform(post("/permissions/details").with(csrf())
                                            .accept(MediaType.APPLICATION_XML)
                                            .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotAcceptable());
    }

    @Test
    @WithMockUser
    void givenEmptyPostBody_permissionRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permissions/details").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(""))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    @WithMockUser
    void givenIncompletePostBody_permissionRequest_returnsBadRequest() throws Exception {
        // Given
        var requestJson = "{\"permissionIds\":[\"" + permissionId + "\"],\"accessToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.\"}";

        // When
        mockMvc.perform(post("/permissions/details").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message",
                                   allOf(iterableWithSize(1), hasItem("handshakeUrl: must not be blank"))));
    }

    @Test
    @WithMockUser
    void givenValidInput_fetchPermissionDetails_callsService() throws Exception {
        // Given
        when(permissionService.fetchPermissionDetails(any())).thenReturn(List.of());
        var requestJson = "{\"eddieId\":\"" + eddieId + "\", \"permissionIds\":[\"" + permissionId + "\"],\"handshakeUrl\":\"http://localhost:8080/region-connectors/aiida/permission-request/41d0a13e-688a-450d-acab-7a6b2951cde2\",\"accessToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.\"}";

        // When
        mockMvc.perform(post("/permissions/details").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
               .andExpect(status().isOk());

        // Then
        verify(permissionService).fetchPermissionDetails(argThat(dto -> dto.permissionIds()
                                                                        .getFirst()
                                                                        .equals(permissionId)));
    }

    @Test
    @WithMockUser
    void givenInvalidMediaType_updatePermission_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_NDJSON))
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser
    void givenInvalidAcceptMediaType_updatePermission_returnsNotAcceptable() throws Exception {
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .accept(MediaType.APPLICATION_XML)
                                                                          .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotAcceptable());
    }

    @Test
    @WithMockUser
    void givenEmptyPatchBody_updatePermission_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(""))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    @WithMockUser
    void givenDuplicateFcaPermissionWithMeterId_fetchPermissionDetails_returnsConflict() throws Exception {
        when(permissionService.fetchPermissionDetails(any()))
                .thenThrow(new ActiveFcaPermissionAlreadyExistsException(permissionId,
                                                                         "003114735",
                                                                         "outbound-aiida"));
        var requestJson = "{\"eddieId\":\"" + eddieId + "\", \"permissionIds\":[\"" + permissionId + "\"],\"handshakeUrl\":\"http://localhost:8080/region-connectors/aiida/permission-request/41d0a13e-688a-450d-acab-7a6b2951cde2\",\"accessToken\":\"******\"}";

        mockMvc.perform(post("/permissions/details")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
               .andExpect(status().isConflict())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString("active outbound FCA permission")))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString("003114735")));
    }

    @Test
    @WithMockUser
    void givenRevoke_updatePermission_callsRevokeOnService() throws Exception {
        // Given
        var requestJson = "{\"operation\": \"REVOKE\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestJson))
               // Then
               .andExpect(status().isOk());

        verify(permissionService).revokePermission(permissionId);
    }

    @Test
    @WithMockUser
    void givenExceptionDuringRevoke_updatePermission_returnsConflict() throws Exception {
        // Given
        var exception = new PermissionStateTransitionException("fooBar", "desired", List.of("allowed"), "current");
        when(permissionService.revokePermission(any())).thenThrow(exception);
        var requestJson = "{\"operation\": \"REVOKE\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestJson))
               // Then
               .andExpect(status().isConflict())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("Cannot transition permission 'fooBar' to state 'desired', as it is not in a one of the permitted states '[allowed]' but in state 'current'")));
    }

    @Test
    @WithMockUser
    void givenInboundDataSourceInUseDuringRevoke_updatePermission_returnsConflict() throws Exception {
        var blockingPermissionId1 = UUID.fromString("a57a9f3d-2b5a-4b84-a32c-51bfa4b865c1");
        var blockingPermissionId2 = UUID.fromString("6dbc4ec9-4f2f-4aa7-b016-36c28945a6e4");
        when(permissionService.revokePermission(any()))
                .thenThrow(new InboundDataSourceInUseException(
                        permissionId,
                        List.of(blockingPermissionId1, blockingPermissionId2)
                ));

        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content("{\"operation\": \"REVOKE\"}"))
               .andExpect(status().isConflict())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString(blockingPermissionId1.toString())))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString(blockingPermissionId2.toString())));
    }

    @Test
    @WithMockUser
    void givenUpdateInboundMessageFormat_updatePermission_callsService() throws Exception {
        // Given
        var requestJson = "{\"operation\": \"UPDATE_INBOUND_MESSAGE_FORMAT\", \"inboundMessageFormat\": \"OPENADR_3_1\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
               // Then
               .andExpect(status().isOk());

        verify(permissionService).updateInboundMessageFormat(permissionId, InboundMessageFormat.OPENADR_3_1);
    }

    @Test
    @WithMockUser
    void givenMissingInboundMessageFormat_updatePermission_returnsBadRequest() throws Exception {
        when(permissionService.updateInboundMessageFormat(permissionId, null))
                .thenThrow(new MissingInboundMessageFormatException());

        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operation\": \"UPDATE_INBOUND_MESSAGE_FORMAT\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("inboundMessageFormat must not be null when operation is UPDATE_INBOUND_MESSAGE_FORMAT.")));
    }

    @Test
    @WithMockUser
    void givenUpdateDisplayName_updatePermission_callsService() throws Exception {
        // Given
        var requestJson = "{\"operation\": \"UPDATE_DISPLAY_NAME\", \"displayName\": \"My smart meter\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
               // Then
               .andExpect(status().isOk());

        verify(permissionService).updateDisplayName(permissionId, "My smart meter");
    }

    @Test
    @WithMockUser
    void givenMissingDisplayName_updatePermission_returnsBadRequest() throws Exception {
        when(permissionService.updateDisplayName(permissionId, null))
                .thenThrow(new MissingDisplayNameException());

        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operation\": \"UPDATE_DISPLAY_NAME\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("displayName must not be blank when operation is UPDATE_DISPLAY_NAME.")));
    }

    @Test
    @WithMockUser
    void givenValidInput_acceptOutboundPermission_callsService_andReturnsCreated() throws Exception {
        // When
        mockMvc.perform(post("/permissions/{permissionId}/accept/outbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(OUTBOUND_ACCEPT_BODY.formatted(eddieId, dataSourceId)))
               // Then
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(permissionService).acceptOutboundPermission(eq(permissionId),
                                                   argThat(dto -> dto.eddieId().equals(eddieId)
                                                                          && dataSourceId.equals(dto.dataSourceId())
                                                                          && dto.accessToken().equals("token")));
    }

    @Test
    @WithMockUser
    void givenValidInput_acceptInboundPermission_callsService_andReturnsCreated() throws Exception {
        // When
        mockMvc.perform(post("/permissions/{permissionId}/accept/inbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INBOUND_ACCEPT_BODY.replace("\"token\"", "\"token\",\"inboundMessageFormat\":\"OPENADR_3_1\"")
                                                            .formatted(eddieId)))
               // Then
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(permissionService).acceptInboundPermission(eq(permissionId),
                                                   argThat(dto -> dto.eddieId().equals(eddieId)
                                                                         && dto.inboundMessageFormat() == InboundMessageFormat.OPENADR_3_1));
    }

    @Test
    @WithMockUser
    void givenMissingHandshakeUrl_acceptOutboundPermission_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permissions/{permissionId}/accept/outbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"eddieId\":\"" + eddieId + "\",\"accessToken\":\"token\",\"dataSourceId\":\"" + dataSourceId + "\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItem("handshakeUrl: must not be blank")));
        verify(permissionService, never()).acceptOutboundPermission(any(), any());
    }

    @Test
    @WithMockUser
    void givenMissingDataSource_acceptOutboundPermission_returnsBadRequest_withoutCallingService() throws Exception {
        mockMvc.perform(post("/permissions/{permissionId}/accept/outbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INBOUND_ACCEPT_BODY.formatted(eddieId)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItem("dataSourceId: must not be null")));
        verify(permissionService, never()).acceptOutboundPermission(any(), any());
    }

    @Test
    @WithMockUser
    void givenInboundPermission_acceptOutboundPermission_returnsBadRequest() throws Exception {
        when(permissionService.acceptOutboundPermission(eq(permissionId), any()))
                .thenThrow(new InvalidOutboundPermissionException(permissionId));

        mockMvc.perform(post("/permissions/{permissionId}/accept/outbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(OUTBOUND_ACCEPT_BODY.formatted(eddieId, dataSourceId)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString("is not an outbound permission")));
    }

    @Test
    @WithMockUser
    void givenOutboundPermission_acceptInboundPermission_returnsBadRequest() throws Exception {
        when(permissionService.acceptInboundPermission(eq(permissionId), any()))
                .thenThrow(new InvalidInboundPermissionException(permissionId));

        mockMvc.perform(post("/permissions/{permissionId}/accept/inbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INBOUND_ACCEPT_BODY.formatted(eddieId)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString("is not an inbound permission")));
    }

    @Test
    @WithMockUser
    void givenIncompatibleDataSource_acceptOutboundPermission_returnsBadRequest() throws Exception {
        when(permissionService.acceptOutboundPermission(eq(permissionId), any()))
                .thenThrow(new IncompatibleDataSourceException("Data source is incompatible."));

        mockMvc.perform(post("/permissions/{permissionId}/accept/outbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(OUTBOUND_ACCEPT_BODY.formatted(eddieId, dataSourceId)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString("incompatible")));
    }

    @Test
    @WithMockUser
    void givenDataSourceNotFound_acceptOutboundPermission_returnsNotFound() throws Exception {
        when(permissionService.acceptOutboundPermission(eq(permissionId), any()))
                .thenThrow(new DataSourceNotFoundException(dataSourceId));

        mockMvc.perform(post("/permissions/{permissionId}/accept/outbound", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(OUTBOUND_ACCEPT_BODY.formatted(eddieId, dataSourceId)))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString(dataSourceId.toString())));
    }

    @Test
    @WithMockUser
    void givenValidInput_rejectPermission_callsService_andReturnsNoContent() throws Exception {
        mockMvc.perform(post("/permissions/{permissionId}/reject", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"handshakeUrl\":\"http://localhost/permission-request/{permissionId}\",\"accessToken\":\"token\",\"reason\":\"Not interested\"}"))
               .andExpect(status().isNoContent());

        verify(permissionService).rejectPermission(eq(permissionId),
                                                   argThat(dto -> "Not interested".equals(dto.reason())));
    }

    @Test
    @WithMockUser
    void givenAlreadyAcceptedPermission_rejectPermission_returnsBadRequest() throws Exception {
        doThrow(new PermissionAlreadyExistsException(permissionId)).when(permissionService)
                                                                    .rejectPermission(eq(permissionId), any());

        mockMvc.perform(post("/permissions/{permissionId}/reject", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"handshakeUrl\":\"http://localhost/permission-request/{permissionId}\",\"accessToken\":\"token\"}"))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void givenRemovedAcceptOperation_updatePermission_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operation\": \"ACCEPT\"}"))
               .andExpect(status().isBadRequest());
    }

    private List<Permission> sampleDataForGetAllPermissionsTest() {
        var permission1 = new Permission(eddieId, permissionId, "https://example.org", "accessToken", userId);
        permission1.setGrantTime(grant);

        grant = grant.plusSeconds(1000);
        var permission2 = new Permission(eddieId, permissionId, "https://example.org", "accessToken", userId);
        permission2.setGrantTime(grant);

        grant = grant.plusSeconds(5000);
        var permission3 = new Permission(eddieId, permissionId, "https://example.org", "accessToken", userId);
        permission3.setGrantTime(grant);

        // grant time order is permission3, permission2, permission1
        return List.of(permission3, permission2, permission1);
    }

    @TestConfiguration
    static class PermissionControllerTestConfiguration {
        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Bean
        public JsonMapperBuilderCustomizer objectMapperCustomizer() {
            return new AiidaConfiguration().objectMapperCustomizer();
        }

        @Bean
        public ConnectionStatusEventStream connectionStatusEventStream() {
            return new ConnectionStatusEventStream();
        }

        @Bean
        public EventStream eventStream() {
            return new EventStream();
        }

        @Bean
        public ConnectionStatusService connectionStatusService(StreamerManager streamerManager) {
            return new ConnectionStatusService(Clock.systemUTC(), streamerManager);
        }
    }
}
