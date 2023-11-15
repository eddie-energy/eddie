package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaPermissionRequestStateTransitionTest {
    @Mock
    AiidaRegionConnectorService mockService;
    private AiidaPermissionRequest request;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        request = new AiidaPermissionRequest("TestId", "TestConn", "dataNeed",
                now, now.plusSeconds(2000), mockService);
    }

    @Test
    void verify_validate_changesState() {
        assertDoesNotThrow(() -> request.validate());
        assertEquals(PermissionProcessStatus.VALIDATED, request.state().status());
    }

    void verify_validate_sendToPA() {
        assertDoesNotThrow(() -> request.validate());
        assertDoesNotThrow(() -> request.sendToPermissionAdministrator());
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, request.state().status());
        verify(mockService).sendToPermissionAdministrator(any());
    }

    @Test
    void verify_validate_sendToPA_timeOut() {
        verify_validate_sendToPA();
        assertThrows(UnsupportedOperationException.class, () -> request.timeOut());
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, request.state().status());
    }

    @Test
    void verify_validate_sendToPA_reject() {
        verify_validate_sendToPA();
        assertThrows(UnsupportedOperationException.class, () -> request.reject());
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, request.state().status());
    }

    @Test
    void verify_validate_sendToPA_invalid() {
        verify_validate_sendToPA();
        assertDoesNotThrow(() -> request.invalid());
        assertEquals(PermissionProcessStatus.INVALID, request.state().status());
    }

    void verify_validate_sendToPA_accept() {
        verify_validate_sendToPA();
        assertDoesNotThrow(() -> request.accept());
        assertEquals(PermissionProcessStatus.ACCEPTED, request.state().status());
    }

    @Test
    void verify_validate_sendToPA_accept_terminate() {
        verify_validate_sendToPA_accept();
        assertDoesNotThrow(() -> request.terminate());
        assertEquals(PermissionProcessStatus.TERMINATED, request.state().status());
    }

    @Test
    void verify_validate_sendToPA_accept_revoke() {
        verify_validate_sendToPA_accept();
        assertDoesNotThrow(() -> request.revoke());
        assertEquals(PermissionProcessStatus.REVOKED, request.state().status());
    }

    @Test
    void verify_validate_sendToPA_accept_timeLimit() {
        verify_validate_sendToPA_accept();
        assertDoesNotThrow(() -> request.timeLimit());
        assertEquals(PermissionProcessStatus.TIME_LIMIT, request.state().status());
    }

    // TODO this only tests the happy path
}