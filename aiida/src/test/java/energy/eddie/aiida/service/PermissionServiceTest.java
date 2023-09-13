package energy.eddie.aiida.service;

import energy.eddie.aiida.dto.PermissionDto;
import energy.eddie.aiida.model.permission.KafkaStreamingConfig;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.model.permission.PermissionStatus;
import energy.eddie.aiida.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    @Mock
    private PermissionRepository repository;
    @InjectMocks
    private PermissionService service;
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
    private String uuid;

    @BeforeEach
    void setUp() {
        uuid = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
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
    }

    @Nested
    @DisplayName("Test service layer set up of a new permission")
    class PermissionServiceSetupNewPermissionTest {
        @Test
        void givenValidInput_setupPermission_asExpected() {
            var permissionDto = new PermissionDto(serviceName, start, expiration, grant, connectionId, codes, streamingConfig);

            when(repository.save(any(Permission.class))).then(i -> {
                var arg = ((Permission) i.getArgument(0));
                ReflectionTestUtils.setField(arg, "permissionId", uuid);
                return arg;
            });

            Permission newPermission = service.setupNewPermission(permissionDto);

            assertEquals(uuid, newPermission.permissionId());
            assertEquals(serviceName, newPermission.serviceName());
            assertEquals(start, newPermission.startTime());
            assertEquals(expiration, newPermission.expirationTime());
            assertEquals(grant, newPermission.grantTime());
            assertEquals(connectionId, newPermission.connectionId());
            assertThat(codes).hasSameElementsAs(newPermission.requestedCodes());
            assertEquals(bootstrapServers, newPermission.kafkaStreamingConfig().bootstrapServers());
            assertEquals(validDataTopic, newPermission.kafkaStreamingConfig().dataTopic());
            assertEquals(validStatusTopic, newPermission.kafkaStreamingConfig().statusTopic());
            assertEquals(validSubscribeTopic, newPermission.kafkaStreamingConfig().subscribeTopic());
            assertEquals(PermissionStatus.ACCEPTED, newPermission.status());
            assertNull(newPermission.revokeTime());

            verify(repository, atLeastOnce()).save(any(Permission.class));
        }
    }
}