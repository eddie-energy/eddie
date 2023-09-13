package energy.eddie.aiida.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.model.permission.KafkaStreamingConfig;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
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