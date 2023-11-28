package energy.eddie.aiida.models.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionTest {
    private Instant start;
    private Instant expiration;
    private Instant grant;
    private String permissionId;
    private String name;
    private String connectionId;
    private Set<String> codes;
    private String bootstrapServers;
    private String validDataTopic;
    private String validStatusTopic;
    private String validSubscribeTopic;
    private KafkaStreamingConfig streamingConfig;

    @BeforeEach
    void setUp() {
        // valid parameters
        permissionId = "f69f9bc2-e16c-4de4-8c3e-00d219dcd819";
        name = "My Test Service";
        connectionId = "RandomId";
        start = Instant.now();
        expiration = start.plusSeconds(5000);

        bootstrapServers = "localhost:9092";
        validDataTopic = "ValidPublishTopic";
        validStatusTopic = "ValidStatusTopic";
        validSubscribeTopic = "ValidSubscribeTopic";
        streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        codes = Set.of("1.8.0", "2.8.0");
        grant = Instant.now();
    }

    @Test
    void givenNull_updateStatus_throws() {
        var permission = new Permission(permissionId, name, start, expiration, grant, connectionId, codes, streamingConfig);
        assertThrows(NullPointerException.class, () -> permission.updateStatus(null));
    }

    @Test
    void givenNull_revokeTime_throws() {
        var permission = new Permission(permissionId, name, start, expiration, grant, connectionId, codes, streamingConfig);
        assertThrows(NullPointerException.class, () -> permission.revokeTime(null));
    }

    @Test
    void givenRevocationTimeBeforeGrantTime_revokeTime_throws() {
        var permission = new Permission(permissionId, name, start, expiration, grant, connectionId, codes, streamingConfig);
        var revokeTime = start.minusSeconds(1000);

        assertThrows(IllegalArgumentException.class, () -> permission.revokeTime(revokeTime));
    }

    @Test
    void givenValidRevocationTime_asExpected() {
        // Given
        var permission = new Permission(permissionId, name, start, expiration, grant, connectionId, codes, streamingConfig);
        var revocationTime = start.plusSeconds(1000);

        // When
        permission.revokeTime(revocationTime);
        permission.updateStatus(PermissionStatus.REVOKED);

        // Then
        assertEquals(revocationTime, permission.revokeTime());
        assertEquals(PermissionStatus.REVOKED, permission.status());

        // no other fields should have been modified
        assertEquals(permissionId, permission.permissionId());
        assertEquals(name, permission.serviceName());
        assertEquals(start, permission.startTime());
        assertEquals(expiration, permission.expirationTime());
        assertEquals(grant, permission.grantTime());
        assertEquals(connectionId, permission.connectionId());
        assertEquals(permissionId, permission.permissionId());
        assertThat(codes).hasSameElementsAs(permission.requestedCodes());
        assertEquals(bootstrapServers, permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permission.kafkaStreamingConfig().subscribeTopic());
    }
}
