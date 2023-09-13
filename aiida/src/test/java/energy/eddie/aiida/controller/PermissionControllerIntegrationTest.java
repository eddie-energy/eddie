package energy.eddie.aiida.controller;

import energy.eddie.aiida.model.permission.Permission;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
