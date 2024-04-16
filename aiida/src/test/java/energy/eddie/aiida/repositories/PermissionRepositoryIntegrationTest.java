package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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

    @Test
    void givenPermission_save_returnsInstanceWithAllFieldsSet() {
        var start = Instant.now().plusSeconds(100_000);
        var end = start.plusSeconds(400_000);
        var permissionId = UUID.randomUUID().toString();
        var codes = Set.of("1.8.0", "2.8.0");

        String name = "My NewAIIDA Test Service";
        String connectionId = "NewAiidaRandomConnectionId";
        String dataNeedId = "dataNeedId";
        Instant grant = Instant.now();
        Permission permission = new Permission(permissionId, name, dataNeedId, start, end,
                                               grant, connectionId, codes);

        var savedPermission = permissionRepository.save(permission);

        assertEquals(permissionId, savedPermission.permissionId());
        assertEquals(name, savedPermission.serviceName());
        assertEquals(start, savedPermission.startTime());
        assertEquals(end, savedPermission.expirationTime());
        assertEquals(grant, savedPermission.grantTime());
        assertEquals(connectionId, savedPermission.connectionId());
        assertEquals(dataNeedId, savedPermission.dataNeedId());
        assertNull(savedPermission.revokeTime());
        assertEquals(PermissionStatus.ACCEPTED, savedPermission.status());

        assertThat(codes).hasSameElementsAs(savedPermission.requestedCodes());
    }

    @Test
    void givenRevokedPermission_save_asExpected() {
        var start = Instant.now().plusSeconds(100_000);
        var end = start.plusSeconds(400_000);
        var permissionId = UUID.randomUUID().toString();
        var codes = Set.of("1.8.0", "2.8.0");

        String name = "My NewAIIDA Test Service";
        String connectionId = "NewAiidaRandomConnectionId";
        String dataNeedId = "dataNeedId";
        Instant grant = Instant.now();
        Instant revokeTime = grant.plusSeconds(1000);

        Permission permission = new Permission(permissionId, name, dataNeedId, start, end, grant,
                                               connectionId, codes);

        assertEquals(PermissionStatus.ACCEPTED, permission.status());

        permission.updateStatus(PermissionStatus.REVOKED);
        permission.revokeTime(revokeTime);

        var savedPermission = permissionRepository.save(permission);

        assertEquals(permissionId, savedPermission.permissionId());
        assertEquals(name, savedPermission.serviceName());
        assertEquals(start, savedPermission.startTime());
        assertEquals(end, savedPermission.expirationTime());
        assertEquals(grant, savedPermission.grantTime());
        assertEquals(connectionId, savedPermission.connectionId());
        assertEquals(dataNeedId, savedPermission.dataNeedId());
        assertEquals(revokeTime, savedPermission.revokeTime());
        assertEquals(PermissionStatus.REVOKED, savedPermission.status());
        assertThat(codes).hasSameElementsAs(savedPermission.requestedCodes());
    }
}
