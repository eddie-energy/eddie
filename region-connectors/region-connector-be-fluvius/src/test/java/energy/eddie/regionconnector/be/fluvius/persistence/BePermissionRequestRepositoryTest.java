package energy.eddie.regionconnector.be.fluvius.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.CreatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BePermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private BePermissionRequestRepository permissionRequestRepository;
    @Autowired
    private BePermissionEventRepository permissionEventRepository;

    @Test
    void testFindTimedOutPermissionRequests_findTimedOutPermissionRequests() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        permissionEventRepository.saveAndFlush(new CreatedEvent("pid",
                                                                "cid",
                                                                "dnid",
                                                                now.minusHours(25)));
        permissionEventRepository.saveAndFlush(new SimpleEvent("pid",
                                                               PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        permissionEventRepository.saveAndFlush(new CreatedEvent("otherPid",
                                                                "cid",
                                                                "dnid",
                                                                now));
        permissionEventRepository.saveAndFlush(new SimpleEvent("otherPid",
                                                               PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));


        // When
        var res = permissionRequestRepository.findStalePermissionRequests(24);

        // Then
        assertEquals(1, res.size());
        var pr = res.getFirst();
        assertEquals("pid", pr.permissionId());
    }
}