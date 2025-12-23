package energy.eddie.outbound.metric.repositories;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.model.PermissionRequestStatusDurationModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DirtiesContext
class PermissionRequestStatusDurationRepositoryTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private PermissionRequestStatusDurationRepository prStatusDurationRepository;

    @Test
    void getMedianDurationTest() {
        // Given
        List<PermissionRequestStatusDurationModel> prStatusDurations = new ArrayList<>();
        prStatusDurations.add(new PermissionRequestStatusDurationModel("pid1", PermissionProcessStatus.CREATED,
                98, "dnId", "paId", "rcId", "CC"));
        prStatusDurations.add(new PermissionRequestStatusDurationModel("pid2", PermissionProcessStatus.CREATED,
                100, "dnId", "paId", "rcId", "CC"));
        prStatusDurationRepository.saveAll(prStatusDurations);

        // When
        double median = prStatusDurationRepository.getMedianDurationMilliseconds(PermissionProcessStatus.CREATED.name(),
                "dnId","paId", "rcId", "CC");

        // Then
        assertThat(median).isEqualTo(99.0);
    }
}
