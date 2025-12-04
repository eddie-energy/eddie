package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionEventRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UnableToSendEventTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private DeEtaPermissionEventRepository eventRepository;

    @Test
    void testPersist_unableToSendEvent_fieldsPersisted() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pid = "pid-u2s";
        var cid = "cid-123";
        var dnid = "dn-456";
        var reason = "HTTP 500 Internal Server Error; body: boom";
        var evt = new UnableToSendEvent(pid, cid, dnid, reason, now);

        // When
        var saved = eventRepository.saveAndFlush(evt);

        // Then
        assertNotNull(saved);
        var latest = eventRepository.findTop2ByPermissionIdAndEventCreatedLessThanEqualOrderByEventCreatedDesc(pid, now.plusSeconds(1));
        assertFalse(latest.isEmpty());
        var first = latest.getFirst();
        assertEquals(PermissionProcessStatus.UNABLE_TO_SEND, first.status());
        assertEquals(pid, first.permissionId());

        assertTrue(first instanceof UnableToSendEvent);
        var u2s = (UnableToSendEvent) first;
        assertEquals(reason, u2s.reason());
    }
}
