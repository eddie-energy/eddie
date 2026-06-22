// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.errors.GlobalExceptionHandler;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.IncompatibleDataSourceException;
import energy.eddie.aiida.errors.permission.InboundDataSourceInUseException;
import energy.eddie.aiida.errors.permission.MissingInboundMessageFormatException;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.services.PermissionService;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PermissionController.class)
@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {
    private final UUID eddieId = UUID.fromString("31d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID permissionId = UUID.fromString("41d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID dataSourceId = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID userId = UUID.randomUUID();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionService permissionService;
    @Mock
    private Permission mockPermission;
    private Instant grant;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        grant = Instant.now();

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

        var response = mapper.readValue(responseString, new TypeReference<List<Permission>>() {});
        assertEquals(0, response.size());
    }

    @Test
    @WithMockUser
    void getAllPermissionsByGrantDate_returnsCorrectOrder() throws Exception {
        var permissions = sampleDataForGetAllPermissionsTest();
        when(permissionService.getAllPermissionsSortedByGrantTime()).thenReturn(permissions);

        var responseString = mockMvc.perform(get("/permissions"))
                                    .andExpect(status().isOk())
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString();

        var response = mapper.readValue(responseString, new TypeReference<List<Permission>>() {});

        assertEquals(3, response.size());
        assertNull(response.getFirst().handshakeUrl());
        assertEquals(permissionId, response.getFirst().id());
        assertEquals(PermissionStatus.CREATED, permissions.get(1).status());
        assertEquals(eddieId, permissions.get(2).eddieId());
    }

    @Test
    @WithMockUser
    void getActiveInboundPermissions_returnsPermissionsWithDataSource() throws Exception {
        when(permissionService.getActiveInboundPermissions()).thenReturn(List.of(mockPermission));

        mockMvc.perform(get("/permissions/inbound/active"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1));

        verify(permissionService).getActiveInboundPermissions();
    }

    @Test
    @WithMockUser
    void givenInvalidMediaType_permissionRequest_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/permissions").with(csrf()).contentType(MediaType.APPLICATION_CBOR))
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser
    void givenInvalidAcceptMediaType_permissionRequest_returnsNotAcceptable() throws Exception {
        mockMvc.perform(post("/permissions").with(csrf())
                                            .accept(MediaType.APPLICATION_XML)
                                            .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotAcceptable());
    }

    @Test
    @WithMockUser
    void givenEmptyPostBody_permissionRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permissions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(""))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    @WithMockUser
    void givenIncompletePostBody_permissionRequest_returnsBadRequest() throws Exception {
        // Given
        when(permissionService.setupNewPermissions(any())).thenReturn(List.of(mockPermission));
        var requestJson = "{\"permissionIds\":[\"" + permissionId + "\"],\"accessToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.\"}";

        // When
        mockMvc.perform(post("/permissions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message",
                                   allOf(iterableWithSize(1), hasItem("handshakeUrl: must not be blank"))));
    }

    @Test
    @WithMockUser
    void givenValidInput_setupPermission_callsService() throws Exception {
        // Given
        when(permissionService.setupNewPermissions(any())).thenReturn(List.of(mockPermission));
        var requestJson = "{\"eddieId\":\"" + eddieId + "\", \"permissionIds\":[\"" + permissionId + "\"],\"handshakeUrl\":\"http://localhost:8080/region-connectors/aiida/permission-request/41d0a13e-688a-450d-acab-7a6b2951cde2\",\"accessToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.\"}";

        // When
        mockMvc.perform(post("/permissions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
               .andExpect(status().isCreated());

        // Then
        verify(permissionService).setupNewPermissions(argThat(dto -> dto.permissionIds()
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
    void givenAccept_updatePermission_callsAcceptOnService() throws Exception {
        // Given
        var requestJson = "{\"operation\": \"ACCEPT\", \"dataSourceId\": \"" + dataSourceId + "\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestJson))
               // Then
               .andExpect(status().isOk());

        verify(permissionService).acceptPermission(permissionId, dataSourceId, null);
    }

    @Test
    @WithMockUser
    void givenAcceptWithInboundMessageFormat_updatePermission_callsAcceptOnService() throws Exception {
        // Given
        var requestJson = "{\"operation\": \"ACCEPT\", \"inboundMessageFormat\": \"OPENADR_3_1\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
               // Then
               .andExpect(status().isOk());

        verify(permissionService).acceptPermission(permissionId, null, InboundMessageFormat.OPENADR_3_1);
    }

    @Test
    @WithMockUser
    void givenReject_updatePermission_callsRejectOnService() throws Exception {
        // Given
        var requestJson = "{\"operation\": \"REJECT\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestJson))
               // Then
               .andExpect(status().isOk());

        verify(permissionService).rejectPermission(permissionId);
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
    void givenIncompatibleDataSource_updatePermission_returnsBadRequest() throws Exception {
        when(permissionService.acceptPermission(permissionId, dataSourceId, null))
                .thenThrow(new IncompatibleDataSourceException("Data source is incompatible."));

        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operation\": \"ACCEPT\", \"dataSourceId\": \"" + dataSourceId + "\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString("incompatible")));
    }

    @Test
    @WithMockUser
    void givenDataSourceNotFound_updatePermission_returnsNotFound() throws Exception {
        when(permissionService.acceptPermission(permissionId, dataSourceId, null))
                .thenThrow(new DataSourceNotFoundException(dataSourceId));

        mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operation\": \"ACCEPT\", \"dataSourceId\": \"" + dataSourceId + "\"}"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString(dataSourceId.toString())));
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
    }
}
