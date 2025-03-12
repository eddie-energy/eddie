package energy.eddie.regionconnector.cds.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.CreatedEvent;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.permission.requests.OAuthRequestType;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CdsPermissionRequestRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CdsPermissionRequestRepository repository;
    @Autowired
    private CdsPermissionEventRepository eventRepository;


    @Test
    void testFindTimedOutPermissionRequests_findTimedOutPermissionRequests() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        eventRepository.saveAndFlush(new CreatedEvent("pid",
                                                      "cid",
                                                      "dnid",
                                                      1,
                                                      now.minusHours(25)));
        eventRepository.saveAndFlush(new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        eventRepository.saveAndFlush(new CreatedEvent("otherPid",
                                                      "cid",
                                                      "dnid",
                                                      1,
                                                      now));
        eventRepository.saveAndFlush(new SimpleEvent("otherPid",
                                                     PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));


        // When
        var res = repository.findStalePermissionRequests(24);

        // Then
        assertThat(res)
                .singleElement()
                .extracting(CdsPermissionRequest::permissionId)
                .isEqualTo("pid");
    }

    @Test
    void testFindTimedOutPermissionRequests_wherePushedAuthorizationRequestTimedOut() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        eventRepository.saveAndFlush(new CreatedEvent("pid",
                                                      "cid",
                                                      "dnid",
                                                      1,
                                                      now));
        eventRepository.saveAndFlush(new SentToPaEvent("pid",
                                                       now.minusHours(1),
                                                       "",
                                                       OAuthRequestType.PUSHED_AUTHORIZATION_REQUEST));


        // When
        var res = repository.findStalePermissionRequests(24);

        // Then
        assertThat(res)
                .singleElement()
                .extracting(CdsPermissionRequest::permissionId)
                .isEqualTo("pid");
    }
}