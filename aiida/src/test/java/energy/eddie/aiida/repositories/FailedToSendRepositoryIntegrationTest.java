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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@Testcontainers
class FailedToSendRepositoryIntegrationTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer timescale =
            new PostgreSQLContainer(
                    DockerImageName.parse("timescale/timescaledb:latest-pg17")
                                   .asCompatibleSubstituteFor("postgres"))
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("timescaledb/create-aiida-db-and-emqx-user.sql"),
                            "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql"
                    );
    @Autowired
    private FailedToSendRepository failedToSendRepository;

    @Test
    void retryQueries_boundRowsPermissionAndSnapshot() {
        UUID permissionId = UUID.randomUUID();
        var rows = new ArrayList<FailedToSendEntity>();
        for (int i = 0; i < 103; i++) {
            rows.add(new FailedToSendEntity(permissionId, "topic", new byte[]{1}));
        }
        failedToSendRepository.saveAllAndFlush(rows);
        long ceiling = Objects.requireNonNull(
                failedToSendRepository.findTopByPermissionIdOrderByIdDesc(permissionId)).id();
        failedToSendRepository.saveAndFlush(new FailedToSendEntity(UUID.randomUUID(), "other", new byte[]{2}));
        failedToSendRepository.saveAndFlush(new FailedToSendEntity(permissionId, "later", new byte[]{3}));

        var page1 = failedToSendRepository.findTop100ByPermissionIdAndIdGreaterThanAndIdLessThanEqualOrderByIdAsc(
                permissionId, 0, ceiling);
        var page2 = failedToSendRepository.findTop100ByPermissionIdAndIdGreaterThanAndIdLessThanEqualOrderByIdAsc(
                permissionId, page1.getLast().id(), ceiling);
        assertEquals(100, page1.size());
        assertEquals(3, page2.size());
        assertEquals(ceiling, page2.getLast().id());
        assertTrue(
                Stream.concat(page1.stream(), page2.stream())
                        .allMatch(row -> row.permissionId().equals(permissionId) && row.id() <= ceiling));
    }


    @Test
    void emptyPermissionHasNoRetrySnapshot() {
        assertNull(failedToSendRepository.findTopByPermissionIdOrderByIdDesc(UUID.randomUUID()));
    }
}
