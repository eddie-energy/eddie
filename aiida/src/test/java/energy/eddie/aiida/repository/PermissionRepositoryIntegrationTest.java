package energy.eddie.aiida.repository;

import energy.eddie.aiida.model.permission.KafkaStreamingConfig;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.model.permission.PermissionStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"     // TODO: once AIIDA is more final, use a custom schema
})
class PermissionRepositoryIntegrationTest {
    static PostgreSQLContainer<?> timescale = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                    .asCompatibleSubstituteFor("postgres")
    );

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    TestEntityManager entityManager;

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

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();
    }

    @Test
    void testThatDbSetsPermissionId() {
        var start = Instant.now().plusSeconds(100_000);
        var end = start.plusSeconds(400_000);

        var codes = Set.of("1.8.0", "2.8.0");
        var validDataTopic = "ValidPublishTopic";
        var validStatusTopic = "ValidStatusTopic";
        var validSubscribeTopic = "ValidSubscribeTopic";
        var bootstrapServers = "localhost:9092";
        var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        String name = "My NewAIIDA Test Service";
        String connectionId = "NewAiidaRandomConnectionId";
        Instant grant = Instant.now();
        Permission permission = new Permission(name, start, end, grant,
                connectionId, codes, streamingConfig);
        // DB should set permissionId
        assertNull(permission.permissionId());

        permissionRepository.save(permission);
        assertNotNull(permission.permissionId());

        assertEquals(name, permission.serviceName());
        assertEquals(start, permission.startTime());
        assertEquals(end, permission.expirationTime());
        assertEquals(grant, permission.grantTime());
        assertEquals(connectionId, permission.connectionId());
        assertNull(permission.terminateTime());
        assertEquals(PermissionStatus.ACCEPTED, permission.status());

        assertThat(codes).hasSameElementsAs(permission.requestedCodes());
        assertEquals(bootstrapServers, permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permission.kafkaStreamingConfig().subscribeTopic());
    }

    @Test
    void testWithTerminatedPermission() {
        var start = Instant.now().plusSeconds(100_000);
        var end = start.plusSeconds(400_000);

        var codes = Set.of("1.8.0", "2.8.0");
        var validDataTopic = "ValidPublishTopic";
        var validStatusTopic = "ValidStatusTopic";
        var validSubscribeTopic = "ValidSubscribeTopic";
        var bootstrapServers = "localhost:9092";
        var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        String name = "My NewAIIDA Test Service";
        String connectionId = "NewAiidaRandomConnectionId";
        Instant grant = Instant.now();
        Instant terminateTime = grant.plusSeconds(1000);

        Permission permission = new Permission(name, start, end, grant,
                connectionId, codes, streamingConfig);
        // DB should set permissionId
        assertNull(permission.permissionId());
        assertEquals(PermissionStatus.ACCEPTED, permission.status());

        permission.updateStatus(PermissionStatus.REVOKED);
        permission.terminateTime(terminateTime);

        permissionRepository.save(permission);
        assertNotNull(permission.permissionId());

        assertEquals(name, permission.serviceName());
        assertEquals(start, permission.startTime());
        assertEquals(end, permission.expirationTime());
        assertEquals(grant, permission.grantTime());
        assertEquals(connectionId, permission.connectionId());
        assertEquals(terminateTime, permission.terminateTime());
        assertEquals(PermissionStatus.REVOKED, permission.status());

        assertThat(codes).hasSameElementsAs(permission.requestedCodes());
        assertEquals(bootstrapServers, permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permission.kafkaStreamingConfig().subscribeTopic());
    }
}

