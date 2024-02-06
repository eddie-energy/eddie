package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisAcceptedState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisPendingAcknowledgmentState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisRejectedState;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PermissionRequestServiceTest {
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest");
    @Autowired
    private PermissionRequestService permissionRequestService;
    @Autowired
    private PermissionRequestRepository<TimeframedPermissionRequest> repository;

    @BeforeAll
    static void beforeAll() {
        postgreSQLContainer.start();
    }

    @AfterAll
    static void afterAll() {
        postgreSQLContainer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("region-connector.fr.enedis.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("region-connector.fr.enedis.datasource.username", postgreSQLContainer::getUsername);
        registry.add("region-connector.fr.enedis.datasource.password", postgreSQLContainer::getPassword);
    }

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws StateTransitionException {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);
        var request = new PermissionRequestForCreation("cid", "dnid", start, end);

        // When
        var res = permissionRequestService.createPermissionRequest(request);
        var permissionRequest = repository.findByPermissionId(res.permissionId());

        // Then
        assertTrue(permissionRequest.isPresent());
        assertEquals("cid", permissionRequest.get().connectionId());
        assertEquals(FrEnedisPendingAcknowledgmentState.class, permissionRequest.get().state().getClass());
    }

    @Test
    @DirtiesContext
    void testAuthorizePermissionRequest_acceptsPermissionRequest() throws StateTransitionException, PermissionNotFoundException {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        request.changeState(new FrEnedisPendingAcknowledgmentState(request));
        repository.save(request);


        // When
        permissionRequestService.authorizePermissionRequest("pid", "upid");
        // Then
        assertEquals(FrEnedisAcceptedState.class, request.state().getClass());
    }

    @Test
    @DirtiesContext
    void testAuthorizePermissionRequestWithNullUsageId_rejectsPermissionRequest() throws StateTransitionException, PermissionNotFoundException {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        request.changeState(new FrEnedisPendingAcknowledgmentState(request));
        repository.save(request);


        // When
        permissionRequestService.authorizePermissionRequest("pid", null);
        // Then
        assertEquals(FrEnedisRejectedState.class, request.state().getClass());
    }

    @Test
    void testAuthorizePermissionRequest_withNonExistingPermissionRequest_throws() {
        // Given, When, Then
        assertThrows(PermissionNotFoundException.class, () -> permissionRequestService.authorizePermissionRequest("pid", "upid"));
    }

    @Test
    @DirtiesContext
    void testFindConnectionStatusById_returnsConnectionStatus() {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        repository.save(request);

        // When
        var res = permissionRequestService.findConnectionStatusMessageById("pid");

        // Then
        assertTrue(res.isPresent());
        assertEquals("pid", res.get().permissionId());
    }

    @Test
    @DirtiesContext
    void testFindConnectionStatusById_withNotExistingPermissionId_returnsEmpty() {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        repository.save(request);

        // When
        var res = permissionRequestService.findConnectionStatusMessageById("asdfasdfadsf");

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    @DirtiesContext
    void testFindPermissionRequestById_returnsPermissionRequest() {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        repository.save(request);

        // When
        var res = permissionRequestService.findPermissionRequestByPermissionId("pid");

        // Then
        assertTrue(res.isPresent());
        assertEquals(request.permissionId(), res.get().permissionId());
    }

    @Test
    @DirtiesContext
    void testFindPermissionRequestById_withNotExistingPermissionId_returnsEmpty() {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        repository.save(request);

        // When
        var res = permissionRequestService.findPermissionRequestByPermissionId("asdfasdfadsf");

        // Then
        assertTrue(res.isEmpty());
    }
}