package energy.eddie.aiida.service;

import energy.eddie.aiida.dto.PermissionDto;
import energy.eddie.aiida.error.InvalidPermissionRevocationException;
import energy.eddie.aiida.error.PermissionNotFoundException;
import energy.eddie.aiida.model.permission.KafkaStreamingConfig;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.model.permission.PermissionStatus;
import energy.eddie.aiida.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    @Mock
    private PermissionRepository repository;
    @Mock
    private Clock clock;
    @InjectMocks
    private PermissionService service;
    private Permission permission;
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
        grant = Instant.parse("2023-08-01T10:00:00.00Z");
        start = grant.plusSeconds(100_000);
        expiration = start.plusSeconds(800_000);
        serviceName = "My NewAIIDA Test Service";
        connectionId = "NewAiidaRandomConnectionId";
        codes = Set.of("1.8.0", "2.8.0");

        bootstrapServers = "localhost:9092";
        validDataTopic = "ValidPublishTopic";
        validStatusTopic = "ValidStatusTopic";
        validSubscribeTopic = "ValidSubscribeTopic";

        streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);
        permission = new Permission(serviceName, start, expiration, grant, connectionId, codes, streamingConfig);
    }

    @Test
    void givenNotExistingPermission_findById_throws() {
        var notExistingId = "NotExistingId";

        when(repository.findById(notExistingId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.findById(notExistingId));
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

    @Nested
    @DisplayName("Test service layer revocation of a permission")
    class PermissionServiceRevocationTest {
        @Test
        void givenNotExistingPermissionId_revokePermission_throws() {
            var notExistingId = "NotExistingId";

            when(repository.findById(notExistingId)).thenReturn(Optional.empty());

            assertThrows(PermissionNotFoundException.class, () -> service.revokePermission(notExistingId));
        }

        @ParameterizedTest
        @EnumSource(
                value = PermissionStatus.class,
                names = {"ACCEPTED", "WAITING_FOR_START", "STREAMING_DATA"},
                mode = EnumSource.Mode.EXCLUDE)
        void givenPermissionWithInvalidStatus_revokePermission_throws(PermissionStatus status) {
            when(repository.findById(uuid)).then(i -> {
                ReflectionTestUtils.setField(permission, "status", status);
                return Optional.of(permission);
            });

            assertThrows(InvalidPermissionRevocationException.class, () -> service.revokePermission(uuid));
        }

        @ParameterizedTest
        @EnumSource(
                value = PermissionStatus.class,
                names = {"ACCEPTED", "WAITING_FOR_START", "STREAMING_DATA"},
                mode = EnumSource.Mode.INCLUDE)
        void givenValidPermission_revokePermission_asExpected(PermissionStatus status) {
            Instant revokeTime = Instant.parse("2023-09-13T10:15:30.00Z");
            when(clock.instant()).thenReturn(revokeTime);

            ReflectionTestUtils.setField(permission, "permissionId", uuid);
            ReflectionTestUtils.setField(permission, "status", status);

            when(repository.findById(uuid)).thenReturn(Optional.of(permission));
            when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));

            Permission revokedPermission = service.revokePermission(uuid);

            // these fields mustn't have been modified
            assertEquals(uuid, revokedPermission.permissionId());
            assertEquals(serviceName, revokedPermission.serviceName());
            assertEquals(start, revokedPermission.startTime());
            assertEquals(expiration, revokedPermission.expirationTime());
            assertEquals(grant, revokedPermission.grantTime());
            assertEquals(connectionId, revokedPermission.connectionId());

            assertThat(codes).hasSameElementsAs(revokedPermission.requestedCodes());

            assertEquals(bootstrapServers, revokedPermission.kafkaStreamingConfig().bootstrapServers());
            assertEquals(validDataTopic, revokedPermission.kafkaStreamingConfig().dataTopic());
            assertEquals(validStatusTopic, revokedPermission.kafkaStreamingConfig().statusTopic());
            assertEquals(validSubscribeTopic, revokedPermission.kafkaStreamingConfig().subscribeTopic());

            // this changed
            assertEquals(PermissionStatus.REVOKED, revokedPermission.status());
            assertEquals(revokeTime, revokedPermission.revokeTime());


            verify(repository, atLeastOnce()).findById(uuid);
            verify(repository, atLeastOnce()).save(any(Permission.class));
        }

        @Test
        void givenRevokeTimeBeforeGrantTime_revokePermission_throws() {
            Instant grantTime = Instant.parse("2023-10-01T10:00:00.00Z");
            Instant revokeTime = Instant.parse("2023-09-01T10:00:00.00Z");
            when(clock.instant()).thenReturn(revokeTime);

            ReflectionTestUtils.setField(permission, "permissionId", uuid);
            ReflectionTestUtils.setField(permission, "grantTime", grantTime);

            when(repository.findById(uuid)).thenReturn(Optional.of(permission));

            assertThrows(IllegalArgumentException.class, () -> service.revokePermission(uuid));
        }
    }
}