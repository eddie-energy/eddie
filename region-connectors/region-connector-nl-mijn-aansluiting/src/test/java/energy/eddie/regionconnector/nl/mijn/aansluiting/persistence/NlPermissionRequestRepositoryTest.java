package energy.eddie.regionconnector.nl.mijn.aansluiting.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlCreatedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
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
class NlPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private NlPermissionRequestRepository permissionRequestRepository;
    @Autowired
    private MijnAansluitingPermissionEventRepository permissionEventRepository;

    @Test
    void testFindTimedOutPermissionRequests_findTimedOutPermissionRequests() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        permissionEventRepository.saveAndFlush(new NlCreatedEvent("pid", "cid", "dnid", now.minusHours(25)));
        permissionEventRepository.saveAndFlush(new NlSimpleEvent("pid", PermissionProcessStatus.VALIDATED));
        permissionEventRepository.saveAndFlush(new NlCreatedEvent("otherPid", "cid", "dnid", now));
        permissionEventRepository.saveAndFlush(new NlSimpleEvent("otherPid", PermissionProcessStatus.VALIDATED));


        // When
        var res = permissionRequestRepository.findStalePermissionRequests(24);

        // Then
        assertEquals(1, res.size());
        var pr = res.getFirst();
        assertEquals("pid", pr.permissionId());
    }
}