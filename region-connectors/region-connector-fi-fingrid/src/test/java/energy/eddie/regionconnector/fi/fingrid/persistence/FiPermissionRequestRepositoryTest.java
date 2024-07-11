package energy.eddie.regionconnector.fi.fingrid.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FiPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");
    @Autowired
    private FiPermissionEventRepository eventRepository;
    @Autowired
    private FiPermissionRequestRepository permissionRequestRepository;

    @Test
    void testFindStalePermissionRequests_findsCorrectPermissionRequests() {
        // Given
        var clock = Clock.fixed(Instant.now(Clock.systemUTC()).minus(2, ChronoUnit.HOURS), ZoneOffset.UTC);
        eventRepository.saveAndFlush(new SimpleEvent("pid1", PermissionProcessStatus.VALIDATED, clock));
        eventRepository.saveAndFlush(new SimpleEvent("pid2", PermissionProcessStatus.VALIDATED));

        // When
        var prs = permissionRequestRepository.findStalePermissionRequests(1);

        // Then
        assertAll(
                () -> assertEquals(1, prs.size()),
                () -> assertEquals("pid1", prs.getFirst().permissionId())
        );
    }
}