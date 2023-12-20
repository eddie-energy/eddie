package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisAcceptedState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisPendingAcknowledgmentState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisRejectedState;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PermissionRequestServiceTest {
    @Autowired
    private PermissionRequestService permissionRequestService;
    @Autowired
    private PermissionRequestRepository<TimeframedPermissionRequest> repository;

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
        var end = ZonedDateTime.now(ZoneOffset.UTC);
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
        var end = ZonedDateTime.now(ZoneOffset.UTC);
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
        var end = ZonedDateTime.now(ZoneOffset.UTC);
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
        var end = ZonedDateTime.now(ZoneOffset.UTC);
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
        var end = ZonedDateTime.now(ZoneOffset.UTC);
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
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        repository.save(request);

        // When
        var res = permissionRequestService.findPermissionRequestByPermissionId("asdfasdfadsf");

        // Then
        assertTrue(res.isEmpty());
    }
}