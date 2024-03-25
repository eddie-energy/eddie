package energy.eddie.aiida.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.errors.InvalidPermissionRevocationException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.aiida.errors.PermissionStartFailedException;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.services.PermissionService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.aiida.controllers.GlobalExceptionHandler.ERRORS_JSON_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PermissionController.class)
class PermissionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private Instant start;
    private Instant expiration;
    private Instant grant;
    private String serviceName;
    private String permissionId;
    private String connectionId;
    private String dataNeedId;
    private Set<String> codes;
    private String bootstrapServers;
    private String validDataTopic;
    private String validStatusTopic;
    private String validSubscribeTopic;
    private KafkaStreamingConfig streamingConfig;
    private PermissionDto permissionDto;
    private ObjectMapper mapper;
    @MockBean
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
        start = Instant.now().plusSeconds(100_000);
        expiration = start.plusSeconds(800_000);
        grant = Instant.now();
        serviceName = "My NewAIIDA Test Service";
        connectionId = "NewAiidaRandomConnectionId";
        dataNeedId = "SomeDataNeedId";
        codes = Set.of("1.8.0", "2.8.0");

        bootstrapServers = "localhost:9092";
        validDataTopic = "ValidPublishTopic";
        validStatusTopic = "ValidStatusTopic";
        validSubscribeTopic = "ValidSubscribeTopic";
        streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        permissionDto = new PermissionDto(permissionId, serviceName, dataNeedId, start, expiration, grant, connectionId, codes, streamingConfig);

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void givenErrorWhileSendingConnectionStatusMessage_returnsInternalServerError() throws Exception {
        when(permissionService.setupNewPermission(ArgumentMatchers.any(PermissionDto.class)))
                .thenThrow(new PermissionStartFailedException(mock(Permission.class)));

        var json = mapper.writeValueAsString(permissionDto);

        mockMvc.perform(post("/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Failed to start permission, please try again later.")));
    }

    @Test
    void givenValidInput_asExpected() throws Exception {
        when(permissionService.setupNewPermission(ArgumentMatchers.any(PermissionDto.class))).thenAnswer(i -> {
            PermissionDto dto = (PermissionDto) i.getArguments()[0];
            return new Permission(dto.permissionId(), dto.serviceName(), dto.dataNeedId(), dto.startTime(), dto.expirationTime(),
                    dto.grantTime(), dto.connectionId(), dto.requestedCodes(), dto.kafkaStreamingConfig());
        });

        var json = mapper.writeValueAsString(permissionDto);

        var responseString = mockMvc.perform(
                        post("/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("location", "/permissions/" + permissionId))
                .andReturn().getResponse().getContentAsString();

        var response = mapper.readValue(responseString, Permission.class);

        verify(permissionService, atLeast(1)).setupNewPermission(ArgumentMatchers.any(PermissionDto.class));

        assertEquals(permissionId, response.permissionId());
        assertEquals(PermissionStatus.ACCEPTED, response.status());
        assertEquals(serviceName, response.serviceName());
        assertEquals(start, response.startTime());
        assertEquals(expiration, response.expirationTime());
        assertEquals(grant, response.grantTime());
        assertEquals(connectionId, response.connectionId());
        assertEquals(dataNeedId, response.dataNeedId());
        assertNull(response.revokeTime());
        assertThat(codes).hasSameElementsAs(response.requestedCodes());

        assertEquals(bootstrapServers, response.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, response.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, response.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, response.kafkaStreamingConfig().subscribeTopic());
    }

    @Nested
    @DisplayName("Test setup new permission")
    class PermissionRequestTest {
        @Test
        void givenInvalidMediaType_permissionRequest_returnsUnsupportedMediaType() throws Exception {
            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_CBOR))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void givenInvalidAcceptMediaType_permissionRequest_returnsNotAcceptable() throws Exception {
            mockMvc.perform(post("/permissions")
                            .accept(MediaType.APPLICATION_XML)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotAcceptable());
        }

        @Test
        void givenEmptyPostBody_permissionRequest_returnsBadRequest() throws Exception {
            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
        }

        @Test
        void givenIncompletePostBody_permissionRequest_returnsBadRequest() throws Exception {
            var json = mapper.writeValueAsString(permissionDto);

            // remove a required field to create an invalid request
            json = json.replace("startTime", "INVALID_FIELD_NAME");

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));


            // remove some required fields by renaming the JSON keys
            json = mapper.writeValueAsString(permissionDto)
                    .replace("connectionId", "INVALID_FIELD_NAME2")
                    .replace("serviceName", "INVALID_FIELD_NAME3")
                    .replace("dataTopic", "INVALID_FIELD_NAME4");


            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
        }

        @Test
        void givenNullField_permissionRequest_returnsBadRequest() throws Exception {
            var invalidDto = new PermissionDto(permissionId, serviceName, dataNeedId, null, null,
                    grant, connectionId, codes, streamingConfig);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(4)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItems(
                            "startTime: must not be null.",
                            "expirationTime: must not be null.",
                            "permissionDto: expirationTime must not be null.",
                            "permissionDto: startTime and expirationTime must not be null."
                    )));
        }

        @Test
        void givenInvalidStreamingConfig_permissionRequest_returnsBadRequest() throws Exception {
            var invalidConfig = new KafkaStreamingConfig(bootstrapServers, null, " ", "");
            var invalidDto = new PermissionDto(permissionId, serviceName, dataNeedId, start, expiration, grant, connectionId,
                    codes, invalidConfig);


            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(3)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItems(
                            "kafkaStreamingConfig.subscribeTopic: must not be null or blank.",
                            "kafkaStreamingConfig.statusTopic: must not be null or blank.",
                            "kafkaStreamingConfig.dataTopic: must not be null or blank."
                    )));
        }

        @Test
        void givenExpirationTimeBeforeStartTime_permissionRequest_returnsBadRequest() throws Exception {
            expiration = start.minusSeconds(1000);

            permissionDto = new PermissionDto(permissionId, serviceName, dataNeedId, start, expiration, grant,
                    connectionId, codes, streamingConfig);

            var json = mapper.writeValueAsString(permissionDto);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("permissionDto: expirationTime has to be after startTime.")));
        }

        @Test
        void givenNoPermissionId_permissionRequest_returnsBadRequest() throws Exception {
            permissionDto = new PermissionDto(null, serviceName, dataNeedId, start, expiration, grant,
                    connectionId, codes, streamingConfig);

            var json = mapper.writeValueAsString(permissionDto);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("permissionId: must not be null.")));
        }

        @Test
        void givenBlankPermissionId_permissionRequest_returnsBadRequest() throws Exception {
            permissionDto = new PermissionDto("   ", serviceName, dataNeedId, start, expiration, grant,
                    connectionId, codes, streamingConfig);

            var json = mapper.writeValueAsString(permissionDto);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("permissionId: must be an UUID with 36 characters.")));
        }

        @Test
        void givenTooLongPermissionId_permissionRequest_returnsBadRequest() throws Exception {
            permissionId = "aaabbbbccccddddeeeeefffffgggghhhhiii";
            permissionDto = new PermissionDto(permissionId, serviceName, dataNeedId, start, expiration, grant,
                    connectionId, codes, streamingConfig);

            var json = mapper.writeValueAsString(permissionDto);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("permissionId: must be an UUID with 36 characters.")));
        }

        @Test
        void givenExpirationTimeInPast_permissionRequest_returnsBadRequest() throws Exception {
            start = Instant.now().minusSeconds(1000);
            expiration = Instant.now().minusSeconds(500);
            permissionDto = new PermissionDto(permissionId, serviceName, dataNeedId, start, expiration, grant,
                    connectionId, codes, streamingConfig);

            var json = mapper.writeValueAsString(permissionDto);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("permissionDto: expirationTime must not lie in the past.")));
        }
    }

    @Nested
    @DisplayName("Test GET all permissions")
    class GetAllPermissionsTest {

        @Test
        void givenInvalidAcceptMediaType_returnsNotAcceptable() throws Exception {
            mockMvc.perform(get("/permissions")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isNotAcceptable());
        }

        @Test
        void getAllPermissionsByGrantDate_returnsEmptyList() throws Exception {
            List<Permission> empty = Collections.emptyList();
            when(permissionService.getAllPermissionsSortedByGrantTime()).thenReturn(empty);

            var responseString = mockMvc.perform(get("/permissions"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var response = mapper.readValue(responseString, new TypeReference<List<Permission>>() {
            });
            assertEquals(0, response.size());
        }

        @Test
        void getAllPermissionsByGrantDate_returnsCorrectOrder() throws Exception {
            var permissions = sampleDataForGetAllPermissionsTest();
            when(permissionService.getAllPermissionsSortedByGrantTime()).thenReturn(permissions);

            var responseString = mockMvc.perform(get("/permissions"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var response = mapper.readValue(responseString, new TypeReference<List<Permission>>() {
            });

            assertEquals("Service3", response.get(0).serviceName());
            assertEquals("Service2", response.get(1).serviceName());
            assertEquals("Service1", response.get(2).serviceName());
        }

        List<Permission> sampleDataForGetAllPermissionsTest() {
            var start = Instant.now().plusSeconds(100_000);
            var expiration = start.plusSeconds(800_000);
            var grant = Instant.now();
            var connectionId = "NewAiidaRandomConnectionId";
            var dataNeedId = "dataNeedId";
            var codes = Set.of("1.8.0", "2.8.0");
            var bootstrapServers = "localhost:9092";
            var validDataTopic = "ValidPublishTopic";
            var validStatusTopic = "ValidStatusTopic";
            var validSubscribeTopic = "ValidSubscribeTopic";
            var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

            var name = "Service1";
            var permission1 = new Permission(UUID.randomUUID().toString(), name, dataNeedId, start, expiration, grant, connectionId, codes, streamingConfig);

            name = "Service2";
            grant = grant.plusSeconds(1000);
            var permission2 = new Permission(UUID.randomUUID().toString(), name, dataNeedId, start, expiration, grant, connectionId, codes, streamingConfig);

            name = "Service3";
            grant = grant.plusSeconds(5000);
            var permission3 = new Permission(UUID.randomUUID().toString(), name, dataNeedId, start, expiration, grant, connectionId, codes, streamingConfig);

            // grant time order is permission3, permission2, permission1
            return List.of(permission3, permission2, permission1);
        }
    }

    @Nested
    @DisplayName("Test revoke a permission")
    class RevokePermissionTest {
        private String permissionId;
        private String validPatchOperationBody;

        @BeforeEach
        void setUp() {
            permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
            validPatchOperationBody = "{\"operation\": \"REVOKE_PERMISSION\"}";
        }

        @Test
        void givenInvalidMediaType_revokePermission_returnsUnsupportedMediaType() throws Exception {
            mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                            .contentType(MediaType.APPLICATION_NDJSON))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void givenInvalidAcceptMediaType_revokePermission_returnsNotAcceptable() throws Exception {
            mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                            .accept(MediaType.APPLICATION_XML)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotAcceptable());
        }

        @Test
        void givenEmptyPatchBody_revokePermission_returnsBadRequest() throws Exception {
            mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
        }

        @Test
        @Disabled("Cannot be tested right now, as PatchOperation enum only contains one value.")
        void givenInvalidOperation_revokePermission_returnsBadRequest() throws Exception {
            mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"operation\": \"MY_BLA\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("Invalid PatchOperation, permitted values are: REVOKE_PERMISSION.")
                    )));
        }

        @Test
        void givenInvalidPermissionId_revokePermission_returnsNotFound() throws Exception {
            var invalidId = "NotExistingId";

            when(permissionService.revokePermission(invalidId)).then(i -> {
                throw new PermissionNotFoundException(i.getArgument(0));
            });

            mockMvc.perform(
                            patch("/permissions/{permissionId}", invalidId)
                                    .content(validPatchOperationBody)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No permission with ID '%s' found.".formatted(invalidId))));
        }

        @Test
        void givenPermissionInInvalidState_revokePermission_returnsBadRequest() throws Exception {
            when(permissionService.revokePermission(permissionId)).then(i -> {
                throw new InvalidPermissionRevocationException(i.getArgument(0));
            });

            mockMvc.perform(patch("/permissions/{permissionId}", permissionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPatchOperationBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                    .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Permission with ID '" + permissionId + "' cannot be revoked. Only a permission with status " +
                            "ACCEPTED, WAITING_FOR_START or STREAMING_DATA may be revoked.")));
        }

        @Test
        void givenValidPermission_revokePermission_returnsAsExpected() throws Exception {
            var permission = new Permission(permissionId, serviceName, dataNeedId, start, expiration, grant, connectionId, codes, streamingConfig);
            var revokeTime = Instant.now();

            when(permissionService.revokePermission(permissionId)).then(i -> {
                permission.revokeTime(revokeTime);
                permission.updateStatus(PermissionStatus.REVOKED);
                return permission;
            });

            var responseString = mockMvc.perform(patch("/permissions/" + permissionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPatchOperationBody))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var response = mapper.readValue(responseString, Permission.class);

            verify(permissionService, atLeast(1)).revokePermission(any());

            // these fields must not have been modified
            assertEquals(permissionId, response.permissionId());
            assertEquals(revokeTime, response.revokeTime());
            assertEquals(PermissionStatus.REVOKED, response.status());
        }
    }
}
