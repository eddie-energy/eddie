package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.oauth.OAuthCallback;
import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthErrorResponse;
import energy.eddie.regionconnector.us.green.button.permission.events.UsInvalidEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestAuthorizationServiceTest {

    @Mock
    private UsPermissionRequestRepository usPermissionRequestRepository;

    @Mock
    private Outbox outbox;

    @InjectMocks
    private PermissionRequestAuthorizationService authorizationService;

    @Captor
    private ArgumentCaptor<UsSimpleEvent> simpleEventCaptor;

    @Captor
    private ArgumentCaptor<UsInvalidEvent> invalidEventCaptor;

    @Test
    void testAuthorizePermissionRequest_invalidPermissionId() {
        // Given
        var invalidPermissionId = "invalidPermissionId";
        var callback = mock(OAuthCallback.class);

        // When
        when(callback.state()).thenReturn(invalidPermissionId);
        when(usPermissionRequestRepository.existsById(invalidPermissionId)).thenReturn(false);

        // Then
        assertThrows(PermissionNotFoundException.class,
                     () -> authorizationService.authorizePermissionRequest(callback));
    }

    @Test
    void testAuthorizePermissionRequest_rejectedCallback() throws MissingClientIdException, MissingClientSecretException, PermissionNotFoundException {
        // Given
        var permissionId = "permissionId";
        var callback = mock(OAuthCallback.class);

        // When
        when(callback.state()).thenReturn(permissionId);
        when(callback.error()).thenReturn(Optional.of("access_denied"));
        when(callback.isSuccessful()).thenReturn(false);
        when(usPermissionRequestRepository.existsById(permissionId)).thenReturn(true);

        authorizationService.authorizePermissionRequest(callback);

        // Then
        verify(outbox, times(2)).commit(simpleEventCaptor.capture());

        var rejectedEvent = simpleEventCaptor.getValue();
        assertEquals(permissionId, rejectedEvent.permissionId());
        assertEquals(PermissionProcessStatus.REJECTED, rejectedEvent.status());

        verifyNoMoreInteractions(outbox);
    }

    @Test
    void testAuthorizePermissionRequest_invalid() throws MissingClientIdException, MissingClientSecretException, PermissionNotFoundException {
        // Given
        var permissionId = "permissionId";
        var callback = mock(OAuthCallback.class);

        // When
        when(callback.state()).thenReturn(permissionId);
        when(callback.error()).thenReturn(Optional.of("invalid_request"));
        when(callback.isSuccessful()).thenReturn(false);
        when(usPermissionRequestRepository.existsById(permissionId)).thenReturn(true);

        authorizationService.authorizePermissionRequest(callback);

        // Then
        verify(outbox).commit(simpleEventCaptor.capture());
        verify(outbox).commit(invalidEventCaptor.capture());

        UsSimpleEvent simpleEvent = simpleEventCaptor.getValue();
        assertEquals(permissionId, simpleEvent.permissionId());
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, simpleEvent.status());

        UsInvalidEvent invalidEvent = invalidEventCaptor.getValue();
        assertEquals(permissionId, invalidEvent.permissionId());
        assertEquals(OAuthErrorResponse.INVALID_REQUEST, invalidEvent.invalidReason());

        verifyNoMoreInteractions(outbox);
    }
}
