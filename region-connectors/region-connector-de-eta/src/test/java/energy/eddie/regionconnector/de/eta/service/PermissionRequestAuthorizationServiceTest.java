package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.auth.EtaAuthService;
import energy.eddie.regionconnector.de.eta.auth.AuthCallback;
import energy.eddie.regionconnector.de.eta.auth.AuthTokenResponse;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestAuthorizationServiceTest {

    private static final String PERMISSION_ID = "perm-id-123";

    @Mock
    private DePermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @Mock
    private EtaAuthService authService;

    @Spy
    private DeEtaPlusConfiguration configuration = new DeEtaPlusConfiguration(
            "test-party", "http://test-url",
            new DeEtaPlusConfiguration.AuthConfig("test-client", "secret", "tokenUrl", "authUrl",
                                                   "redirectUri",
                                                   "scope"),
            new DeEtaPlusConfiguration.ApiConfig(
                    new DeEtaPlusConfiguration.ApiConfig.ClientConfig("id", "secret"))
    );

    @InjectMocks
    private PermissionRequestAuthorizationService service;

    @Captor
    private ArgumentCaptor<SimpleEvent> simpleEventCaptor;

    @Test
    void authorizePermissionRequestWhenNotFoundShouldThrowException() {
        AuthCallback callback = new AuthCallback(Optional.of("code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authorizePermissionRequest(callback))
                .isInstanceOf(PermissionNotFoundException.class);
    }

    @Test
    void authorizePermissionRequestWhenAlreadyRejectedShouldReturnEarly() throws Exception {
        AuthCallback callback = new AuthCallback(Optional.of("code"), Optional.empty(), PERMISSION_ID);
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .status(PermissionProcessStatus.REJECTED)
                .build();
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));

        service.authorizePermissionRequest(callback);

        verify(outbox, never()).commit(any());
        verify(authService, never()).exchangeCodeForToken(anyString(), anyString());
    }

    @Test
    void authorizePermissionRequestWhenNotValidatedShouldReturnEarly() throws Exception {
        AuthCallback callback = new AuthCallback(Optional.of("code"), Optional.empty(), PERMISSION_ID);
        // Status != REJECTED and != INVALID and != VALIDATED (e.g. ACCEPTED)
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .status(PermissionProcessStatus.ACCEPTED)
                .build();
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));

        service.authorizePermissionRequest(callback);

        verify(outbox, never()).commit(any());
        verify(authService, never()).exchangeCodeForToken(anyString(), anyString());
    }

    @Test
    void authorizePermissionRequestWhenCallbackHasErrorShouldCommitRejected() throws Exception {
        AuthCallback callback = new AuthCallback(Optional.empty(), Optional.of("access_denied"),
                                                   PERMISSION_ID);
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .status(PermissionProcessStatus.VALIDATED)
                .build();
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));

        service.authorizePermissionRequest(callback);

        verify(outbox, times(2)).commit(simpleEventCaptor.capture());

        assertThat(simpleEventCaptor.getAllValues().get(0).status())
                .isEqualTo(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        assertThat(simpleEventCaptor.getAllValues().get(1).status())
                .isEqualTo(PermissionProcessStatus.REJECTED);
        verify(authService, never()).exchangeCodeForToken(anyString(), anyString());
    }

    @Test
    void authorizePermissionRequestWhenSuccessfulAndTokenObtainedShouldCommitAccepted() throws Exception {
        AuthCallback callback = new AuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .status(PermissionProcessStatus.VALIDATED)
                .build();
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));

        AuthTokenResponse tokenResponse = new AuthTokenResponse(
                new AuthTokenResponse.TokenData("access-token", "refresh-token"), true);
        when(authService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.just(tokenResponse));

        service.authorizePermissionRequest(callback);

        verify(outbox).commit(simpleEventCaptor.capture());
        assertThat(simpleEventCaptor.getValue().status())
                .isEqualTo(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        verify(outbox).commit(any(AcceptedEvent.class));
    }

    @Test
    void authorizePermissionRequestWhenTokenExchangeYieldsNullTokenShouldCommitInvalid() throws Exception {
        AuthCallback callback = new AuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .status(PermissionProcessStatus.VALIDATED)
                .build();
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));

        AuthTokenResponse tokenResponse = new AuthTokenResponse(null, true);
        when(authService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.just(tokenResponse));

        service.authorizePermissionRequest(callback);

        verify(outbox, times(2)).commit(simpleEventCaptor.capture());

        assertThat(simpleEventCaptor.getAllValues().get(1).status()).isEqualTo(PermissionProcessStatus.INVALID);
    }

    @Test
    void authorizePermissionRequestWhenTokenExchangeYieldsErrorResponseShouldCommitInvalid() throws Exception {
        AuthCallback callback = new AuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .status(PermissionProcessStatus.VALIDATED)
                .build();
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));

        AuthTokenResponse tokenResponse = new AuthTokenResponse(
                new AuthTokenResponse.TokenData("access", "refresh"),
                false); // success=false
        when(authService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.just(tokenResponse));

        service.authorizePermissionRequest(callback);

        verify(outbox, times(2)).commit(simpleEventCaptor.capture());

        assertThat(simpleEventCaptor.getAllValues().get(1).status()).isEqualTo(PermissionProcessStatus.INVALID);
    }

    @Test
    void authorizePermissionRequestWhenTokenExchangeFailsWithExceptionShouldCommitInvalid() throws Exception {
        AuthCallback callback = new AuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .status(PermissionProcessStatus.VALIDATED)
                .build();
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));

        when(authService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        service.authorizePermissionRequest(callback);

        verify(outbox, times(2)).commit(simpleEventCaptor.capture());

        assertThat(simpleEventCaptor.getAllValues().get(1).status()).isEqualTo(PermissionProcessStatus.INVALID);
    }
}
