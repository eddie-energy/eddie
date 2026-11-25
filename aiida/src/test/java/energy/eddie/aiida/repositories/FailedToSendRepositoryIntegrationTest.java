// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.FailedToSendEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FailedToSendRepositoryIntegrationTest {
    private static final String TIMESCALEDB_IMAGE = "timescale/timescaledb:latest-pg17";
    private static final String TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE =
            "timescaledb/create-aiida-db-and-emqx-user.sql";
    private static final String TIMESCALEDB_CONTAINER_PATH =
            "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql";

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer timescale =
            new PostgreSQLContainer(
                    DockerImageName.parse(TIMESCALEDB_IMAGE)
                                   .asCompatibleSubstituteFor("postgres"))
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE),
                            TIMESCALEDB_CONTAINER_PATH
                    );

    @Autowired
    private FailedToSendRepository repository;

    @Test
    void deleteAllByPermissionId_withoutTransaction_deletesOnlyMessagesOfPermission() {
        // Given
        var expiredPermissionId = UUID.randomUUID();
        var otherPermissionId = UUID.randomUUID();
        var payload = "{}".getBytes(StandardCharsets.UTF_8);
        repository.save(new FailedToSendEntity(expiredPermissionId, "topic", payload));
        repository.save(new FailedToSendEntity(expiredPermissionId, "topic", payload));
        repository.save(new FailedToSendEntity(otherPermissionId, "topic", payload));

        // When
        repository.deleteAllByPermissionId(expiredPermissionId);

        // Then
        assertTrue(repository.findAllByPermissionId(expiredPermissionId).isEmpty());
        assertEquals(1, repository.findAllByPermissionId(otherPermissionId).size());
    }
}
