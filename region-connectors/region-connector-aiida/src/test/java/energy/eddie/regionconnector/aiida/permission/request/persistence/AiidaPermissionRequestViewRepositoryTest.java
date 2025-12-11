package energy.eddie.regionconnector.aiida.permission.request.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.aiida.permission.request.events.SimpleEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
class AiidaPermissionRequestViewRepositoryTest {
    private static final String CREATE_EDDIE_DB_AND_EMQX_USER_FILE = "create-eddie-db-and-emqx-user.sql";
    private static final String CONTAINER_PATH = "/docker-entrypoint-initdb.d/" + CREATE_EDDIE_DB_AND_EMQX_USER_FILE;

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgresqlContainer =
            new PostgreSQLContainer("postgres:17.5-alpine")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(CREATE_EDDIE_DB_AND_EMQX_USER_FILE),
                            CONTAINER_PATH
                    );

    @Autowired
    private AiidaPermissionRequestViewRepository permissionRequestRepository;
    @Autowired
    private AiidaPermissionEventRepository permissionEventRepository;

    @Test
    void testFindTimedOutPermissionRequests_findTimedOutPermissionRequests() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var clock = Clock.fixed(Instant.now(Clock.systemUTC()).minus(25, ChronoUnit.HOURS), ZoneOffset.UTC);
        permissionEventRepository.saveAndFlush((PermissionEvent) new CreatedEvent("pid",
                                                                                  "cid",
                                                                                  "dnid",
                                                                                  now.toLocalDate(),
                                                                                  now.plusDays(10).toLocalDate(),
                                                                                  "termination-topic",
                                                                                  clock));
        permissionEventRepository.saveAndFlush((PermissionEvent) new SimpleEvent("pid",
                                                                                 PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        permissionEventRepository.saveAndFlush((PermissionEvent) new CreatedEvent("otherPid",
                                                                                  "cid",
                                                                                  "dnid",
                                                                                  now.toLocalDate(),
                                                                                  now.plusDays(20).toLocalDate(),
                                                                                  "termination-topic"));
        permissionEventRepository.saveAndFlush((PermissionEvent) new SimpleEvent("otherPid",
                                                                                 PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));


        // When
        var res = permissionRequestRepository.findStalePermissionRequests(24);

        // Then
        assertEquals(1, res.size());
        var pr = res.getFirst();
        assertEquals("pid", pr.permissionId());
    }
}