package energy.eddie.aiida.controllers;

import energy.eddie.aiida.dtos.PatchOperation;
import energy.eddie.aiida.dtos.PatchPermissionDto;
import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static energy.eddie.aiida.TestUtils.getKafkaConfig;
import static energy.eddie.aiida.TestUtils.getKafkaConsumer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"     // TODO: once AIIDA is more final, use a custom schema
})
@Testcontainers
@EnableScheduling
class PermissionControllerIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> timescale = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                    .asCompatibleSubstituteFor("postgres")
    );
    @Container
    @ServiceConnection
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.1"));
    @Autowired
    DataSource dataSource;
    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;

    private PermissionDto getPermissionDto(Instant start, Instant expiration, TestInfo testInfo) {
        var name = "My NewAIIDA Test Service";
        var grant = Instant.now();
        var connectionId = "NewAiidaRandomConnectionId";
        var codes = Set.of("1.8.0", "2.8.0");
        var streamingConfig = getKafkaConfig(testInfo, kafka);

        return new PermissionDto(name, start, expiration, grant, connectionId, codes, streamingConfig);
    }

    // Truncate DB script doesn't work with @Sql annotation, so execute it manually to ensure clean DB for each test
    @AfterEach
    void cleanUp() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("clean_database.sql"));
        populator.execute(dataSource);
    }

    @Test
    @Sql(scripts = {"/setupPermission_insertSamplePermissions.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
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

    @Test
    void givenInvalidInput_setupNewPermission_returnsBadRequest(TestInfo testInfo) {
        String expected = "{\"errors\":[\"expirationTime has to be after startTime.\"]}";

        var start = Instant.now().plusSeconds(100_000);
        var expiration = start.minusSeconds(200_000);
        var dto = getPermissionDto(start, expiration, testInfo);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(getPermissionsUrl(),
                dto, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(expected, responseEntity.getBody());
    }

    /**
     * Tests that
     * <li> a new permission is set up successfully
     * <li> it's the only permission returned by getPermissions (previously empty database)
     */
    @Test
    void givenValidInput_setupNewPermission_asExpected_andGetPermissionsReturnsOnlyThisPermission(TestInfo testInfo) {
        var start = Instant.now().plusSeconds(100_000);
        var expiration = start.plusSeconds(200_000);
        var dto = getPermissionDto(start, expiration, testInfo);

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

    /**
     * Tests that
     * <li> a new permission is set up successfully
     * <li> the <i>ACCEPTED</i> status message is received by the Kafka cluster
     * <li> the permission is revoked successfully (response fields are as expected)
     * <li> the <i>REVOCATION_RECEIVED</i> and <i>REVOCATION</i> status messages are received by the Kafka cluster
     */
    @Test
    @Timeout(10)
    void givenValidInput_setupNewPermission_andRevokePermission_asExpected(TestInfo testInfo) {
        var start = Instant.now().plusSeconds(100_000);
        var expiration = start.plusSeconds(200_000);
        var dto = getPermissionDto(start, expiration, testInfo);


        // create the permission
        ResponseEntity<Permission> responseEntity = restTemplate.postForEntity(getPermissionsUrl(),
                dto, Permission.class);
        var permission = responseEntity.getBody();
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(permission);
        var permissionId = permission.permissionId();


        // revoke the permission
        var revokeDto = new PatchPermissionDto(PatchOperation.REVOKE_PERMISSION);
        RequestEntity<PatchPermissionDto> revokeRequest = RequestEntity
                .patch(getPermissionsUrl() + "/" + permissionId)
                .accept(MediaType.APPLICATION_JSON)
                .body(revokeDto);

        // use custom factory because default one does not support Patch operation
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        responseEntity = restTemplate.exchange(revokeRequest, Permission.class);
        permission = responseEntity.getBody();

        assertNotNull(permission);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(permissionId, permission.permissionId());
        assertEquals(PermissionStatus.REVOKED, permission.status());
        var revokeTime = permission.revokeTime();
        assertNotNull(revokeTime);
        assertTrue(revokeTime.isBefore(Instant.now()));


        // check that the status messages have been sent to Kafka
        var consumer = getKafkaConsumer(testInfo, kafka);
        consumer.subscribe(List.of(permission.kafkaStreamingConfig().statusTopic()));
        var polledRecords = new ArrayList<ConsumerRecord<String, String>>();
        while (polledRecords.size() < 3) {
            for (ConsumerRecord<String, String> received : consumer.poll(Duration.ofSeconds(1))) {
                polledRecords.add(received);
            }
        }

        assertEquals(3, polledRecords.size());

        assertThat(polledRecords.get(0).value(), endsWith("\"status\":\"ACCEPTED\"}"));
        assertThat(polledRecords.get(1).value(), endsWith("\"status\":\"REVOCATION_RECEIVED\"}"));
        assertThat(polledRecords.get(2).value(), endsWith("\"status\":\"REVOKED\"}"));

        consumer.close();
    }

    private void assertDtoAndPermission(PermissionDto dto, Permission permission) {
        assertEquals(dto.serviceName(), permission.serviceName());

        // database saves with less precision for nanoseconds, therefore just compare millis
        assertEquals(dto.startTime().toEpochMilli(), permission.startTime().toEpochMilli());
        assertEquals(dto.expirationTime().toEpochMilli(), permission.expirationTime().toEpochMilli());
        assertEquals(dto.grantTime().toEpochMilli(), permission.grantTime().toEpochMilli());

        assertEquals(dto.connectionId(), permission.connectionId());
        org.assertj.core.api.Assertions.assertThat(dto.requestedCodes()).hasSameElementsAs(permission.requestedCodes());
        assertEquals(dto.kafkaStreamingConfig().bootstrapServers(), permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(dto.kafkaStreamingConfig().dataTopic(), permission.kafkaStreamingConfig().dataTopic());
        assertEquals(dto.kafkaStreamingConfig().statusTopic(), permission.kafkaStreamingConfig().statusTopic());
        assertEquals(dto.kafkaStreamingConfig().subscribeTopic(), permission.kafkaStreamingConfig().subscribeTopic());
        assertEquals(PermissionStatus.STREAMING_DATA, permission.status());
        assertNull(permission.revokeTime());
    }

    @Test
    @Sql(scripts = {"/revokePermission_insertSamplePermissions.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void givenPermissionInInvalidState_revokePermission_returnsBadRequest() {
        var permissionId = "592c372e-bced-45b7-a4a9-5f39e66b8d30";
        var dto = new PatchPermissionDto(PatchOperation.REVOKE_PERMISSION);

        String expected = "{\"errors\":[\"Permission with id 592c372e-bced-45b7-a4a9-5f39e66b8d30 cannot be revoked. Only a permission with status ACCEPTED, WAITING_FOR_START or STREAMING_DATA may be revoked.\"]}";

        RequestEntity<PatchPermissionDto> request = RequestEntity
                .patch(getPermissionsUrl() + "/" + permissionId)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto);

        // use custom factory because default one does not support Patch operation
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        HttpClientErrorException.BadRequest badRequestException = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> restTemplate.exchange(request, String.class));

        assertEquals(HttpStatus.BAD_REQUEST, badRequestException.getStatusCode());
        assertEquals(expected, badRequestException.getResponseBodyAsString());
    }

    @Test
    @Sql(scripts = {"/revokePermission_insertSamplePermissions.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void givenPermissionInValidState_revokePermission_asExpected() {
        var permissionId = "1a1c5995-71fc-4078-acd3-46027a2faa51";

        var dto = new PatchPermissionDto(PatchOperation.REVOKE_PERMISSION);

        RequestEntity<PatchPermissionDto> request = RequestEntity
                .patch(getPermissionsUrl() + "/" + permissionId)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto);


        // use custom factory because default one does not support Patch operation
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        ResponseEntity<Permission> responseEntity = restTemplate.exchange(request, Permission.class);
        var permission = responseEntity.getBody();

        assertNotNull(permission);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(permissionId, permission.permissionId());
        assertEquals(PermissionStatus.REVOKED, permission.status());
        var revokeTime = permission.revokeTime();
        assertNotNull(revokeTime);
        assertTrue(revokeTime.isBefore(Instant.now()));
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
