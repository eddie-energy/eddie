package energy.eddie.aiida.controller;

import energy.eddie.aiida.dto.PermissionDto;
import energy.eddie.aiida.model.permission.KafkaStreamingConfig;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.model.permission.PermissionStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"     // TODO: once AIIDA is more final, use a custom schema
})
class PermissionControllerIntegrationTest {
    static PostgreSQLContainer<?> timescale = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                    .asCompatibleSubstituteFor("postgres")
    );

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void beforeAll() {
        timescale.start();
    }

    @AfterAll
    static void afterAll() {
        timescale.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", timescale::getJdbcUrl);
        registry.add("spring.datasource.username", timescale::getUsername);
        registry.add("spring.datasource.password", timescale::getPassword);
    }

    @Test
    @Sql(scripts = {"/setupPermission_insertSamplePermissions.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/setupPermission_insertSamplePermissions_cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getPermissionsByGrantedDate_asExpected() {
        ResponseEntity<List<Permission>> responseEntity = restTemplate.exchange(getPermissionsUrl(),
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                }, new Object[]{});

        List<Permission> permissions = responseEntity.getBody();

        assertNotNull(permissions);
        assertEquals(3, permissions.size());

        // TODO for good tests, would have to compare every single field here??
        assertEquals("Service3", permissions.get(0).serviceName());
        assertEquals("Service1", permissions.get(1).serviceName());
        assertEquals("Service2", permissions.get(2).serviceName());
    }

    @Test
    void getPermissionsByGrantedDate_withEmptyDatabase_asExpected() {
        ResponseEntity<List<Permission>> responseEntity = restTemplate.exchange(getPermissionsUrl(),
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                }, new Object[]{});

        List<Permission> permissions = responseEntity.getBody();

        assertNotNull(permissions);
        assertEquals(0, permissions.size());
    }

    private static PermissionDto getPermissionDto(Instant start, Instant expiration) {
        var name = "My NewAIIDA Test Service";
        var grant = Instant.now();
        var connectionId = "NewAiidaRandomConnectionId";
        var codes = Set.of("1.8.0", "2.8.0");
        var bootstrapServers = "localhost:9092";
        var validDataTopic = "ValidPublishTopic";
        var validStatusTopic = "ValidStatusTopic";
        var validSubscribeTopic = "ValidSubscribeTopic";
        var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        return new PermissionDto(name, start, expiration, grant, connectionId, codes, streamingConfig);
    }

    @Test
    void givenInvalidInput_setupNewPermission_returnsBadRequest() {
        String expected = "{\"errors\":[\"expirationTime has to be after startTime.\"]}";

        var start = Instant.now().plusSeconds(100_000);
        var expiration = start.minusSeconds(200_000);
        var dto = getPermissionDto(start, expiration);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(getPermissionsUrl(),
                dto, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(expected, responseEntity.getBody());
    }

    @Test
    void givenValidInput_setupNewPermission_asExpected_andGetPermissionsReturnsOnlyThisPermission() {
        var start = Instant.now().plusSeconds(100_000);
        var expiration = start.plusSeconds(200_000);
        var dto = getPermissionDto(start, expiration);

        ResponseEntity<Permission> responseEntity = restTemplate.postForEntity(getPermissionsUrl(),
                dto, Permission.class);
        var permission = responseEntity.getBody();

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        assertNotNull(permission);
        // check that permissionId is valid UUID
        Assertions.assertDoesNotThrow(() -> UUID.fromString(Objects.requireNonNull(permission.permissionId())));

        var expectedLocation = "/permissions/" + permission.permissionId();
        assertNotNull(responseEntity.getHeaders().getLocation());
        assertEquals(expectedLocation, responseEntity.getHeaders().getLocation().toString());

        assertDtoAndPermission(dto, permission);

        // get all permissions should now return only one result
        ResponseEntity<List<Permission>> getResponseEntity = restTemplate.exchange(getPermissionsUrl(),
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                }, new Object[]{});

        List<Permission> permissions = getResponseEntity.getBody();

        assertNotNull(permissions);
        assertEquals(1, permissions.size());

        assertDtoAndPermission(dto, permissions.get(0));
    }

    private void assertDtoAndPermission(PermissionDto dto, Permission permission) {
        assertEquals(dto.serviceName(), permission.serviceName());

        // database saves with less precision for nanoseconds, therefore just compare millis
        assertEquals(dto.startTime().toEpochMilli(), permission.startTime().toEpochMilli());
        assertEquals(dto.expirationTime().toEpochMilli(), permission.expirationTime().toEpochMilli());
        assertEquals(dto.grantTime().toEpochMilli(), permission.grantTime().toEpochMilli());

        assertEquals(dto.connectionId(), permission.connectionId());
        assertThat(dto.requestedCodes()).hasSameElementsAs(permission.requestedCodes());
        assertEquals(dto.kafkaStreamingConfig().bootstrapServers(), permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(dto.kafkaStreamingConfig().dataTopic(), permission.kafkaStreamingConfig().dataTopic());
        assertEquals(dto.kafkaStreamingConfig().statusTopic(), permission.kafkaStreamingConfig().statusTopic());
        assertEquals(dto.kafkaStreamingConfig().subscribeTopic(), permission.kafkaStreamingConfig().subscribeTopic());
        assertEquals(PermissionStatus.ACCEPTED, permission.status());
        assertNull(permission.revokeTime());
    }

    private String getPermissionsUrl() {
        return UriComponentsBuilder
                .newInstance()
                .scheme("http")
                .host("localhost")
                .port(port)
                .path("permissions")
                .toUriString();
    }
}
