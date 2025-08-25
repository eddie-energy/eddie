package energy.eddie.outbound.metric.repositories;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PermissionRequestMetricsRepositoryTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private PermissionRequestMetricsRepository permissionRequestMetricsRepository;

    @Test
    void getPermissionRequestMetrics_withResult() {
        // Given
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                0.0,
                0.0,
                PermissionProcessStatus.CREATED,
                "dnType",
                "paId",
                "rcId"
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        var res = permissionRequestMetricsRepository.getPermissionRequestMetrics(
                PermissionProcessStatus.CREATED,
                "dnType",
                "paId",
                "rcId",
                "CC"
        );

        // Then
        assertThat(res).isEqualTo(Optional.of(prMetrics));
    }

    @Test
    void getPermissionRequestMetrics_noResult() {
        // Given
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                0.0,
                0.0,
                PermissionProcessStatus.CREATED,
                "dnType",
                "paId",
                "rcId"
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        var res = permissionRequestMetricsRepository.getPermissionRequestMetrics(
                PermissionProcessStatus.VALIDATED,
                "dnType",
                "paId",
                "rcId",
                "CC"
        );

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void upsertPermissionRequestMetric_exists() {
        // Given
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                90,
                90,
                PermissionProcessStatus.CREATED,
                "dnType",
                "paId",
                "rcId"
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        permissionRequestMetricsRepository.upsertPermissionRequestMetric(
                97.5,
                97.5,
                2,
                PermissionProcessStatus.CREATED.name(),
                "dnType",
                "paId",
                "rcId",
                "CC"
        );

        // Then
        assertThat(permissionRequestMetricsRepository.findAll()).hasSize(1);
    }

    @Test
    void upsertPermissionRequestMetric_notExists() {
        // Given
        PermissionRequestMetricsModel prMetrics = new PermissionRequestMetricsModel(
                90,
                90,
                PermissionProcessStatus.CREATED,
                "dnType",
                "paId",
                "rcId"
        );
        permissionRequestMetricsRepository.save(prMetrics);

        // When
        permissionRequestMetricsRepository.upsertPermissionRequestMetric(
                97.5,
                97.5,
                2,
                PermissionProcessStatus.VALIDATED.name(),
                "dnType",
                "paId",
                "rcId",
                "CC"
        );

        // Then
        assertThat(permissionRequestMetricsRepository.findAll()).hasSize(2);
    }
}
