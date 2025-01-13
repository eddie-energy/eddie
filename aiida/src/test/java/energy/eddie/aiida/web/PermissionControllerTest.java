package energy.eddie.aiida.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.PermissionService;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PermissionController.class)
class PermissionControllerTest {
    private final UUID eddieId = UUID.fromString("31d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID permissionId = UUID.fromString("41d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID userId = UUID.randomUUID();
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionService permissionService;
    @Mock
    private Permission mockPermission;
    private Instant grant;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        grant = Instant.now();

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
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

        assertEquals("Service3", response.get(0).serviceName());
        assertEquals("Service2", response.get(1).serviceName());
        assertEquals("Service1", response.get(2).serviceName());
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
        when(permissionService.setupNewPermission(any())).thenReturn(mockPermission);
        when(mockPermission.permissionId()).thenReturn(permissionId);
        var requestJson = "{\"permissionId\":\"" + permissionId + "\",\"handshakeUrl\":\"http://localhost:8080/region-connectors/aiida/permission-request/41d0a13e-688a-450d-acab-7a6b2951cde2\",\"accessToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.\"}";

        // When
        mockMvc.perform(post("/permissions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message",
                                   allOf(iterableWithSize(1), hasItem("serviceName: must not be blank"))));
    }

    @Test
    @WithMockUser
    void givenValidInput_setupPermission_callsService_andReturnsLocationHeader() throws Exception {
        // Given
        when(permissionService.setupNewPermission(any())).thenReturn(mockPermission);
        when(mockPermission.eddieId()).thenReturn(eddieId);
        when(mockPermission.permissionId()).thenReturn(permissionId);
        var requestJson = "{\"eddieId\":\"" + eddieId + "\", \"permissionId\":\"" + permissionId + "\",\"serviceName\":\"FUTURE_NEAR_REALTIME_DATA\",\"handshakeUrl\":\"http://localhost:8080/region-connectors/aiida/permission-request/41d0a13e-688a-450d-acab-7a6b2951cde2\",\"accessToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.\"}";
        var expectedLocationHeader = "/permissions/" + permissionId;

        // When
        mockMvc.perform(post("/permissions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
               // Then
               .andExpect(status().isCreated()).andExpect(header().string("location", expectedLocationHeader));

        verify(permissionService).setupNewPermission(argThat(dto -> dto.permissionId().equals(permissionId)));
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
        var requestJson = "{\"operation\": \"ACCEPT\"}";

        // When
        mockMvc.perform(patch("/permissions/{permissionId}", permissionId).with(csrf())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestJson))
               // Then
               .andExpect(status().isOk());

        verify(permissionService).acceptPermission(permissionId);
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

    private List<Permission> sampleDataForGetAllPermissionsTest() {
        var name = "Service1";
        var permission1 = new Permission(new QrCodeDto(eddieId,
                                                       permissionId,
                                                       name,
                                                       "https://example.org",
                                                       "fooBarToken"), userId);
        permission1.setGrantTime(grant);

        name = "Service2";
        grant = grant.plusSeconds(1000);
        var permission2 = new Permission(new QrCodeDto(eddieId,
                                                       permissionId,
                                                       name,
                                                       "https://example.org",
                                                       "fooBarToken"), userId);
        permission2.setGrantTime(grant);

        name = "Service3";
        grant = grant.plusSeconds(5000);
        var permission3 = new Permission(new QrCodeDto(eddieId,
                                                       permissionId,
                                                       name,
                                                       "https://example.org",
                                                       "fooBarToken"), userId);
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
