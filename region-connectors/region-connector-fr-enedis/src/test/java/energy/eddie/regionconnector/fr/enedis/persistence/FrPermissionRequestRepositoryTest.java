package energy.eddie.regionconnector.fr.enedis.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.CimTestConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrCreatedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(CimTestConfiguration.class)
class FrPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private FrPermissionRequestRepository permissionRequestRepository;
    @Autowired
    private FrPermissionEventRepository permissionEventRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedsService dataNeedsService;

    @Test
    void testFindTimedOutPermissionRequests_findStalePermissionRequests() {
        // Given
        var fixedClock = Clock.fixed(Instant.now(Clock.systemUTC()).minus(25, ChronoUnit.HOURS), ZoneOffset.UTC);
        permissionEventRepository.saveAll(List.of(
                new FrCreatedEvent("pid", "cid", "dnid", fixedClock),
                new FrSimpleEvent("pid", PermissionProcessStatus.VALIDATED),
                new FrCreatedEvent("otherPid", "cid", "dnid"),
                new FrSimpleEvent("otherPid", PermissionProcessStatus.VALIDATED)
        ));

        // When
        var res = permissionRequestRepository.findStalePermissionRequests(24);

        // Then
        assertEquals(1, res.size());
        var pr = res.getFirst();
        assertEquals("pid", pr.permissionId());
    }
}