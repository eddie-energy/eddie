package energy.eddie.aiida.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.dto.PermissionDto;
import energy.eddie.aiida.model.permission.KafkaStreamingConfig;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.model.permission.PermissionStatus;
import energy.eddie.aiida.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PermissionController.class)
class PermissionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private Instant start;
    private Instant expiration;
    private Instant grant;
    private String serviceName;
    private String connectionId;
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
        start = Instant.now().plusSeconds(100_000);
        expiration = start.plusSeconds(800_000);
        grant = Instant.now();
        serviceName = "My NewAIIDA Test Service";
        connectionId = "NewAiidaRandomConnectionId";
        codes = Set.of("1.8.0", "2.8.0");


        bootstrapServers = "localhost:9092";
        validDataTopic = "ValidPublishTopic";
        validStatusTopic = "ValidStatusTopic";
        validSubscribeTopic = "ValidSubscribeTopic";
        streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        permissionDto = new PermissionDto(serviceName, start, expiration, grant, connectionId, codes, streamingConfig);

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
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
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            // TODO: better error message?
                            hasItem("Failed to read request")
                    )));
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
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("Failed to read request"))));


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
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("Failed to read request")
                    )));
        }

        @Test
        void givenNullField_permissionRequest_returnsBadRequest() throws Exception {
            var invalidDto = new PermissionDto(serviceName, null, null, grant, connectionId,
                    codes, streamingConfig);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(3),
                            hasItem("startTime mustn't be null."),
                            hasItem("expirationTime mustn't be null."),
                            hasItem("startTime and expirationTime mustn't be null.")
                    )));
        }

        @Test
        void givenInvalidStreamingConfig_permissionRequest_returnsBadRequest() throws Exception {
            var invalidConfig = new KafkaStreamingConfig(bootstrapServers, null, " ", "");
            var invalidDto = new PermissionDto(serviceName, start, expiration, grant, connectionId,
                    codes, invalidConfig);


            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(3),
                            hasItem("dataTopic mustn't be null or blank."),
                            hasItem("statusTopic mustn't be null or blank."),
                            hasItem("subscribeTopic mustn't be null or blank.")
                    )));
        }

        @Test
        void givenExpirationTimeBeforeStartTime_permissionRequest_returnsBadRequest() throws Exception {
            expiration = start.minusSeconds(1000);

            permissionDto = new PermissionDto(serviceName, start, expiration, grant, connectionId, codes, streamingConfig);

            var json = mapper.writeValueAsString(permissionDto);

            mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("expirationTime has to be after startTime.")
                    )));
        }

        @Test
        void givenValidInput_asExpected() throws Exception {
            // mock database setting the permissionId
            String uuid = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
            when(permissionService.setupNewPermission(ArgumentMatchers.any(PermissionDto.class))).thenAnswer(i -> {
                PermissionDto dto = (PermissionDto) i.getArguments()[0];
                Permission toSave = new Permission(dto.serviceName(), dto.startTime(), dto.expirationTime(),
                        dto.grantTime(), dto.connectionId(), dto.requestedCodes(), dto.kafkaStreamingConfig());

                ReflectionTestUtils.setField(toSave, "permissionId", uuid);
                return toSave;
            });

            var json = mapper.writeValueAsString(permissionDto);

            var responseString = mockMvc.perform(
                            post("/permissions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("location", "/permissions/" + uuid))
                    .andReturn().getResponse().getContentAsString();

            var response = mapper.readValue(responseString, Permission.class);

            verify(permissionService, atLeast(1)).setupNewPermission(ArgumentMatchers.any(PermissionDto.class));

            assertEquals(uuid, response.permissionId());
            assertEquals(PermissionStatus.ACCEPTED, response.status());
            assertEquals(serviceName, response.serviceName());
            assertEquals(start, response.startTime());
            assertEquals(expiration, response.expirationTime());
            assertEquals(grant, response.grantTime());
            assertEquals(connectionId, response.connectionId());
            assertNull(response.revokeTime());
            assertThat(codes).hasSameElementsAs(response.requestedCodes());

            assertEquals(bootstrapServers, response.kafkaStreamingConfig().bootstrapServers());
            assertEquals(validDataTopic, response.kafkaStreamingConfig().dataTopic());
            assertEquals(validStatusTopic, response.kafkaStreamingConfig().statusTopic());
            assertEquals(validSubscribeTopic, response.kafkaStreamingConfig().subscribeTopic());
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
            var codes = Set.of("1.8.0", "2.8.0");
            var bootstrapServers = "localhost:9092";
            var validDataTopic = "ValidPublishTopic";
            var validStatusTopic = "ValidStatusTopic";
            var validSubscribeTopic = "ValidSubscribeTopic";
            var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

            var name = "Service1";
            var permission1 = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);

            name = "Service2";
            grant = grant.plusSeconds(1000);
            var permission2 = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);

            name = "Service3";
            grant = grant.plusSeconds(5000);
            var permission3 = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);

            // grant time order is permission3, permission2, permission1
            return List.of(permission3, permission2, permission1);
        }
    }
}