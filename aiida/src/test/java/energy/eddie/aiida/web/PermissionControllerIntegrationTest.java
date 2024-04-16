package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.PatchOperation;
import energy.eddie.aiida.dtos.PatchPermissionDto;
import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@EnableScheduling
class PermissionControllerIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> timescale = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                    .asCompatibleSubstituteFor("postgres")
    );
    @Autowired
    private DataSource dataSource;
    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;

    private PermissionDto getPermissionDto(Instant start, Instant expiration) {
        var permissionId = UUID.randomUUID().toString();
        var name = "My NewAIIDA Test Service";
        var grant = Instant.now();
        var connectionId = "NewAiidaRandomConnectionId";
        var dataNeedId = "dataNeedId";
        var codes = Set.of("1.8.0", "2.8.0");

        return new PermissionDto(permissionId, name, dataNeedId, start, expiration, grant, connectionId, codes);
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
    void givenInvalidInput_setupNewPermission_returnsBadRequest() {
        var start = Instant.now().plusSeconds(100_000);
        var expiration = start.minusSeconds(200_000);
        var dto = getPermissionDto(start, expiration);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(getPermissionsUrl(),
                dto, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().contains("permissionDto: expirationTime has to be after startTime."));
        assertTrue(responseEntity.getBody().contains("permissionDto: expirationTime must not lie in the past."));
    }

    /**
     * Tests that
     * <li> a new permission is set up successfully
     * <li> it's the only permission returned by getPermissions (previously empty database)
     */
    @Test
    void givenValidInput_setupNewPermission_asExpected_andGetPermissionsReturnsOnlyThisPermission() {
        var start = Instant.now().minusSeconds(100_000);
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

        assertDtoAndPermission(dto, permissions.getFirst());
    }

    @Test
    void givenPermissionStartInFuture_returnsWaitingForStart() {
        // Given
        var start = Instant.now().plusSeconds(100_000);
        var expiration = start.plusSeconds(200_000);
        var dto = getPermissionDto(start, expiration);

        // When
        ResponseEntity<Permission> responseEntity = restTemplate.postForEntity(getPermissionsUrl(),
                dto, Permission.class);
        var permission = responseEntity.getBody();

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        assertNotNull(permission);
        assertEquals(PermissionStatus.WAITING_FOR_START, permission.status());

        // get all permissions should now return only one result
        ResponseEntity<List<Permission>> getResponseEntity = restTemplate.exchange(getPermissionsUrl(),
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                }, new Object[]{});

        List<Permission> permissions = getResponseEntity.getBody();

        assertNotNull(permissions);
        assertEquals(1, permissions.size());
        assertEquals(PermissionStatus.WAITING_FOR_START, permissions.getFirst().status());
    }

    private void assertDtoAndPermission(PermissionDto dto, Permission permission) {
        assertEquals(dto.serviceName(), permission.serviceName());

        // database saves with less precision for nanoseconds, therefore just compare millis
        assertEquals(dto.startTime().toEpochMilli(), permission.startTime().toEpochMilli());
        assertEquals(dto.expirationTime().toEpochMilli(), permission.expirationTime().toEpochMilli());
        assertEquals(dto.grantTime().toEpochMilli(), permission.grantTime().toEpochMilli());

        assertEquals(dto.connectionId(), permission.connectionId());
        org.assertj.core.api.Assertions.assertThat(dto.requestedCodes()).hasSameElementsAs(permission.requestedCodes());
        assertEquals(PermissionStatus.STREAMING_DATA, permission.status());
        assertNull(permission.revokeTime());
    }

    @Test
    @Sql(scripts = {"/revokePermission_insertSamplePermissions.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void givenPermissionInInvalidState_revokePermission_returnsBadRequest() {
        var permissionId = "592c372e-bced-45b7-a4a9-5f39e66b8d30";
        var dto = new PatchPermissionDto(PatchOperation.REVOKE_PERMISSION);

        String expected = "{\"errors\":[{\"message\":\"Permission with ID '592c372e-bced-45b7-a4a9-5f39e66b8d30' cannot be revoked. Only a permission with status ACCEPTED, WAITING_FOR_START or STREAMING_DATA may be revoked.\"}]}";

        RequestEntity<PatchPermissionDto> request = RequestEntity
                .patch(getPermissionsUrl() + "/" + permissionId)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto);

        // use custom factory because default one does not support Patch operation
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        var badRequestException = assertThrows(HttpClientErrorException.BadRequest.class,
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
