package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.exceptions.InvalidScopesException;
import energy.eddie.regionconnector.us.green.button.oauth.OAuthCallback;
import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthErrorResponse;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAcceptedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsInvalidEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestAuthorizationServiceTest {
    @Mock
    private OAuthCallback callback;
    @Mock
    private UsPermissionRequestRepository usPermissionRequestRepository;
    @Mock
    private Outbox outbox;
    @Mock
    private CredentialService credentialService;
    @InjectMocks
    private PermissionRequestAuthorizationService authorizationService;
    @Captor
    private ArgumentCaptor<UsSimpleEvent> simpleEventCaptor;
    @Captor
    private ArgumentCaptor<UsInvalidEvent> invalidEventCaptor;

    public static Stream<Arguments> testAuthorizePermissionRequest_withInternalException_doesNotEmit() {
        return Stream.of(
                Arguments.of(new MissingClientIdException()),
                Arguments.of(new MissingClientSecretException()),
                Arguments.of(new Exception())
        );
    }

    @Test
    void testAuthorizePermissionRequest_invalidPermissionId() {
        // Given
        var invalidPermissionId = "invalidPermissionId";
        when(callback.state()).thenReturn(invalidPermissionId);
        when(usPermissionRequestRepository.findById(invalidPermissionId))
                .thenReturn(Optional.empty());
        // When
        // Then
        assertThrows(PermissionNotFoundException.class,
                     () -> authorizationService.authorizePermissionRequest(callback));
    }

    @Test
    void testAuthorizePermissionRequest_rejectedCallback() {
        // Given
        var permissionId = "permissionId";
        when(callback.state()).thenReturn(permissionId);
        when(callback.error()).thenReturn(Optional.of("access_denied"));
        when(callback.isSuccessful()).thenReturn(false);
        when(usPermissionRequestRepository.findById(permissionId))
                .thenReturn(Optional.of(createPermissionRequest()));

        // When
        assertThrows(UnauthorizedException.class, () -> authorizationService.authorizePermissionRequest(callback));

        // Then
        verify(outbox, times(2)).commit(simpleEventCaptor.capture());

        var rejectedEvent = simpleEventCaptor.getValue();
        assertEquals(permissionId, rejectedEvent.permissionId());
        assertEquals(PermissionProcessStatus.REJECTED, rejectedEvent.status());

        verifyNoMoreInteractions(outbox);
    }

    @Test
    void testAuthorizePermissionRequest_forAlreadyAcceptedPermissionRequest() throws MissingClientIdException, MissingClientSecretException, PermissionNotFoundException, UnauthorizedException {
        // Given
        var permissionId = "permissionId";
        when(callback.state()).thenReturn(permissionId);
        when(usPermissionRequestRepository.findById(permissionId))
                .thenReturn(Optional.of(createPermissionRequest(PermissionProcessStatus.ACCEPTED)));

        // When
        authorizationService.authorizePermissionRequest(callback);

        // Then
        verify(outbox, never()).commit(any());
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"INVALID", "REJECTED"})
    void testAuthorizePermissionRequest_forAlreadyInvalidOrRejectedPermissionRequest_throws(PermissionProcessStatus status) {
        // Given
        var permissionId = "permissionId";
        when(callback.state()).thenReturn(permissionId);
        when(usPermissionRequestRepository.findById(permissionId))
                .thenReturn(Optional.of(createPermissionRequest(status)));

        // When
        // Then
        assertThrows(UnauthorizedException.class, () -> authorizationService.authorizePermissionRequest(callback));
        verify(outbox, never()).commit(any());
    }

    @Test
    void testAuthorizePermissionRequest_successful() throws MissingClientIdException, MissingClientSecretException, PermissionNotFoundException, UnauthorizedException {
        // Given
        var permissionId = "permissionId";
        when(callback.state()).thenReturn(permissionId);
        when(callback.isSuccessful()).thenReturn(true);
        when(callback.code()).thenReturn(Optional.of("code"));
        when(usPermissionRequestRepository.findById(permissionId))
                .thenReturn(Optional.of(createPermissionRequest()));
        var now = Instant.now(Clock.systemUTC());
        when(credentialService.retrieveAccessToken(any(), any()))
                .thenReturn(Mono.just(new OAuthTokenDetails("pid",
                                                            "token",
                                                            now,
                                                            now.plus(10, ChronoUnit.DAYS),
                                                            "token",
                                                            "1111")));

        // When
        authorizationService.authorizePermissionRequest(callback);

        // Then
        verify(credentialService).retrieveAccessToken(any(), any());
        verify(outbox).commit(isA(UsSimpleEvent.class));
        verify(outbox).commit(isA(UsAcceptedEvent.class));
    }

    @Test
    void testAuthorizePermissionRequest_invalid() {
        // Given
        var permissionId = "permissionId";
        when(callback.state()).thenReturn(permissionId);
        when(callback.error()).thenReturn(Optional.of("invalid_request"));
        when(callback.isSuccessful()).thenReturn(false);
        when(usPermissionRequestRepository.findById(permissionId))
                .thenReturn(Optional.of(createPermissionRequest()));

        // When
        assertThrows(UnauthorizedException.class, () -> authorizationService.authorizePermissionRequest(callback));

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

    @Test
    void testAuthorizePermissionRequest_withMismatchingScopes_emitsInvalid() throws MissingClientIdException, MissingClientSecretException, PermissionNotFoundException, UnauthorizedException {
        // Given
        var permissionId = "permissionId";
        when(callback.state()).thenReturn(permissionId);
        when(callback.isSuccessful()).thenReturn(true);
        when(callback.code()).thenReturn(Optional.of("code"));
        when(usPermissionRequestRepository.findById(permissionId))
                .thenReturn(Optional.of(createPermissionRequest()));
        when(credentialService.retrieveAccessToken(any(), any()))
                .thenReturn(Mono.error(new InvalidScopesException()));

        // When
        authorizationService.authorizePermissionRequest(callback);

        // Then
        verify(outbox).commit(invalidEventCaptor.capture());
        var res = invalidEventCaptor.getValue();
        assertAll(
                () -> assertEquals(PermissionProcessStatus.INVALID, res.status()),
                () -> assertEquals(OAuthErrorResponse.INVALID_SCOPE, res.invalidReason())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAuthorizePermissionRequest_withInternalException_doesNotEmit(Exception e) throws MissingClientIdException, MissingClientSecretException, PermissionNotFoundException, UnauthorizedException {
        // Given
        var permissionId = "permissionId";
        when(callback.state()).thenReturn(permissionId);
        when(callback.isSuccessful()).thenReturn(true);
        when(callback.code()).thenReturn(Optional.of("code"));
        when(usPermissionRequestRepository.findById(permissionId))
                .thenReturn(Optional.of(createPermissionRequest()));
        when(credentialService.retrieveAccessToken(any(), any()))
                .thenReturn(Mono.error(e));

        // When
        authorizationService.authorizePermissionRequest(callback);

        // Then
        verify(outbox, never()).commit(invalidEventCaptor.capture());
    }

    private static GreenButtonPermissionRequest createPermissionRequest() {
        return createPermissionRequest(PermissionProcessStatus.VALIDATED);
    }

    private static GreenButtonPermissionRequest createPermissionRequest(PermissionProcessStatus status) {
        var now = LocalDate.now(ZoneOffset.UTC);
        return new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                status,
                ZonedDateTime.now(ZoneOffset.UTC),
                "US",
                "blbl",
                "http://localhost",
                "other",
                "authId"
        );
    }
}
