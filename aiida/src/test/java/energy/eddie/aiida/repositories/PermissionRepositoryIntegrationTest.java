package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"     // TODO: once AIIDA is more final, use a custom schema
})
@Testcontainers
class PermissionRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> timescale = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                    .asCompatibleSubstituteFor("postgres")
    );

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    TestEntityManager entityManager;

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
        assertNull(permission.revokeTime());
        assertEquals(PermissionStatus.ACCEPTED, permission.status());

        assertThat(codes).hasSameElementsAs(permission.requestedCodes());
        assertEquals(bootstrapServers, permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permission.kafkaStreamingConfig().subscribeTopic());
    }

    @Test
    void givenRevokedPermission_save_asExpected() {
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
        Instant revokeTime = grant.plusSeconds(1000);

        Permission permission = new Permission(name, start, end, grant,
                connectionId, codes, streamingConfig);
        // DB should set permissionId
        assertNull(permission.permissionId());
        assertEquals(PermissionStatus.ACCEPTED, permission.status());

        permission.updateStatus(PermissionStatus.REVOKED);
        permission.revokeTime(revokeTime);

        permissionRepository.save(permission);
        assertNotNull(permission.permissionId());

        assertEquals(name, permission.serviceName());
        assertEquals(start, permission.startTime());
        assertEquals(end, permission.expirationTime());
        assertEquals(grant, permission.grantTime());
        assertEquals(connectionId, permission.connectionId());
        assertEquals(revokeTime, permission.revokeTime());
        assertEquals(PermissionStatus.REVOKED, permission.status());

        assertThat(codes).hasSameElementsAs(permission.requestedCodes());
        assertEquals(bootstrapServers, permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permission.kafkaStreamingConfig().subscribeTopic());
    }
}