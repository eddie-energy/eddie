// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.metric.repositories;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.agnostic.SimpleDataSourceInformation;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DirtiesContext
class PermissionRequestMetricsRepositoryTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private PermissionRequestMetricsRepository permissionRequestMetricsRepository;

    @Test
    void getPermissionRequestMetrics_withResult() {
        // Given
        var dsi = new SimpleDataSourceInformation("CC", "rcId", "mdaId", "paId");
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                0.0,
                0.0,
                PermissionProcessStatus.CREATED,
                "dnType",
                dsi
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        var res = permissionRequestMetricsRepository.getPermissionRequestMetrics(
                PermissionProcessStatus.CREATED,
                "dnType",
                dsi
        );

        // Then
        assertThat(res).isEqualTo(Optional.of(prMetrics));
    }

    @Test
    void getPermissionRequestMetrics_noResult() {
        // Given
        var dsi = new SimpleDataSourceInformation("CC", "rcId", "mdaId", "paId");
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                0.0,
                0.0,
                PermissionProcessStatus.CREATED,
                "dnType",
                dsi
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        var res = permissionRequestMetricsRepository.getPermissionRequestMetrics(
                PermissionProcessStatus.VALIDATED,
                "dnType",
                dsi
        );

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void upsertPermissionRequestMetric_exists() {
        // Given
        var dsi = new SimpleDataSourceInformation("CC", "rcId", "mdaId", "paId");
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                90,
                90,
                PermissionProcessStatus.CREATED,
                "dnType",
                dsi
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        permissionRequestMetricsRepository.upsertPermissionRequestMetric(
                new PermissionRequestMetricsModel(
                        97.5,
                        97.5,
                        2,
                        PermissionProcessStatus.CREATED,
                        "dnType",
                        dsi
                )
        );

        // Then
        assertThat(permissionRequestMetricsRepository.findAll()).hasSize(1);
    }

    @Test
    void upsertPermissionRequestMetric_notExists() {
        // Given
        var dsi = new SimpleDataSourceInformation("CC", "rcId", "mdaId", "paId");
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                90,
                90,
                PermissionProcessStatus.CREATED,
                "dnType",
                dsi
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        permissionRequestMetricsRepository.upsertPermissionRequestMetric(
                new PermissionRequestMetricsModel(
                        97.5,
                        97.5,
                        2,
                        PermissionProcessStatus.VALIDATED,
                        "dnType",
                        dsi
                )
        );

        // Then
        assertThat(permissionRequestMetricsRepository.findAll()).hasSize(2);
    }
}
