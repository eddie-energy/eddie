package energy.eddie.aiida.services;

import energy.eddie.aiida.config.OAuth2SecurityConfiguration;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.cors.CorsConfigurationSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true",
        // manually trigger migration
        "spring.flyway.enabled=false"})
@MockitoBean(types = {ClientRegistrationRepository.class, OAuth2SecurityConfiguration.class, CorsConfigurationSource.class})
@Testcontainers
class PermissionServiceIntegrationTest {
    private static final String TIMESCALEDB_IMAGE = "timescale/timescaledb:latest-pg17";
    private static final String TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE =
            "timescaledb/create-aiida-db-and-emqx-user.sql";
    private static final String TIMESCALEDB_CONTAINER_PATH =
            "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> timescale =
            new PostgreSQLContainer<>(
                    DockerImageName.parse(TIMESCALEDB_IMAGE)
                                   .asCompatibleSubstituteFor("postgres"))
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE),
                            TIMESCALEDB_CONTAINER_PATH
                    );
    private final UUID permissionId = UUID.fromString("8609a9b3-0718-4082-935d-6a98c0f8c5a2");
    @Autowired
    Clock clock;
    @Autowired
    PermissionRepository repository;
    @MockitoSpyBean
    StreamerManager streamerManager;

    /**
     * Create the DB tables with flyway and populate data needed for this testcase. This has to be done in @BeforeAll,
     * to make sure that the DB is populated before the onApplicationEvent method runs, which wouldn't be possible with
     * the @Sql annotation.
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        DriverManagerDataSource dataSource = getDataSource();

        Flyway flyway = Flyway.configure().locations("classpath:db/aiida/migration").dataSource(dataSource).load();
        flyway.migrate();

        Connection conn = dataSource.getConnection();
        ScriptUtils.executeSqlScript(conn, new ClassPathResource("updatePermissionOnStartup.sql"));
        JdbcUtils.closeConnection(conn);
    }

    /**
     * Tests that permissions are queried from the DB on startup and if their expiration time has passed, their status
     * is set accordingly or streaming is started again otherwise.
     */
    @Test
    void givenVariousPermissions_statusAsExpected() {
        var permission = repository.findById(permissionId).orElseThrow();
        assertEquals(PermissionStatus.FULFILLED, permission.status());

        permission = repository.findById(permissionId).orElseThrow();
        assertEquals(PermissionStatus.FULFILLED, permission.status());
    }

    private static DriverManagerDataSource getDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(timescale.getJdbcUrl());
        dataSource.setUsername(timescale.getUsername());
        dataSource.setPassword(timescale.getPassword());
        dataSource.setDriverClassName(timescale.getDriverClassName());
        return dataSource;
    }

    /**
     * Use a TestConfiguration instead of a Mock to override the Bean and its methods on Spring startup and not just
     * when a testcase runs.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public Clock clock() {
            return Clock.fixed(Instant.parse("2023-10-01T12:00:00.00Z"), ZoneId.systemDefault());
        }
    }
}
