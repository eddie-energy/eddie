package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.oauth.EtaOAuthService;
import energy.eddie.regionconnector.de.eta.oauth.OAuthCallback;
import energy.eddie.regionconnector.de.eta.oauth.OAuthTokenResponse;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
    private EtaOAuthService oauthService;
    @Mock
    private DeEtaPlusConfiguration configuration;
    @Mock
    private DePermissionRequest permissionRequest;

    // We only need an empty config mock to return the inner config
    private final DeEtaPlusConfiguration.OAuthConfig oauthConfig = new DeEtaPlusConfiguration.OAuthConfig(
            "test-client", "secret", "tokenUrl", "authUrl", "redirectUri", "scope");

    private PermissionRequestAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new PermissionRequestAuthorizationService(repository, outbox, oauthService, configuration);
    }

    @Test
    void authorizePermissionRequestWhenNotFoundShouldThrowException() {
        OAuthCallback callback = new OAuthCallback(Optional.of("code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authorizePermissionRequest(callback))
                .isInstanceOf(PermissionNotFoundException.class);
    }

    @Test
    void authorizePermissionRequestWhenAlreadyRejectedShouldReturnEarly() throws Exception {
        OAuthCallback callback = new OAuthCallback(Optional.of("code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.REJECTED);

        service.authorizePermissionRequest(callback);

        verify(outbox, never()).commit(any());
        verify(oauthService, never()).exchangeCodeForToken(anyString(), anyString());
    }

    @Test
    void authorizePermissionRequestWhenNotValidatedShouldReturnEarly() throws Exception {
        OAuthCallback callback = new OAuthCallback(Optional.of("code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        // Status != REJECTED and != INVALID and != VALIDATED (e.g. ACCEPTED)
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.ACCEPTED);

        service.authorizePermissionRequest(callback);

        verify(outbox, never()).commit(any());
        verify(oauthService, never()).exchangeCodeForToken(anyString(), anyString());
    }

    @Test
    void authorizePermissionRequestWhenCallbackHasErrorShouldCommitRejected() throws Exception {
        OAuthCallback callback = new OAuthCallback(Optional.empty(), Optional.of("access_denied"), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.VALIDATED);

        service.authorizePermissionRequest(callback);

        ArgumentCaptor<SimpleEvent> captor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox, times(2)).commit(captor.capture());

        assertThat(captor.getAllValues().get(0).status())
                .isEqualTo(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        assertThat(captor.getAllValues().get(1).status()).isEqualTo(PermissionProcessStatus.REJECTED);
        verify(oauthService, never()).exchangeCodeForToken(anyString(), anyString());
    }

    @Test
    void authorizePermissionRequestWhenSuccessfulAndTokenObtainedShouldCommitAccepted() throws Exception {
        OAuthCallback callback = new OAuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.VALIDATED);

        when(configuration.oauth()).thenReturn(oauthConfig);
        OAuthTokenResponse tokenResponse = new OAuthTokenResponse(
                new OAuthTokenResponse.TokenData("access-token", "refresh-token"), true);
        when(oauthService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.just(tokenResponse));

        service.authorizePermissionRequest(callback);

        verify(outbox).commit(any(SimpleEvent.class)); // SENT_TO_PERMISSION_ADMINISTRATOR
        verify(outbox).commit(any(AcceptedEvent.class));
    }

    @Test
    void authorizePermissionRequestWhenTokenExchangeYieldsNullTokenShouldCommitInvalid() throws Exception {
        OAuthCallback callback = new OAuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.VALIDATED);

        when(configuration.oauth()).thenReturn(oauthConfig);
        OAuthTokenResponse tokenResponse = new OAuthTokenResponse(null, true);
        when(oauthService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.just(tokenResponse));

        service.authorizePermissionRequest(callback);

        ArgumentCaptor<SimpleEvent> captor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox, times(2)).commit(captor.capture());

        assertThat(captor.getAllValues().get(1).status()).isEqualTo(PermissionProcessStatus.INVALID);
    }

    @Test
    void authorizePermissionRequestWhenTokenExchangeYieldsErrorResponseShouldCommitInvalid() throws Exception {
        OAuthCallback callback = new OAuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.VALIDATED);

        when(configuration.oauth()).thenReturn(oauthConfig);
        OAuthTokenResponse tokenResponse = new OAuthTokenResponse(
                new OAuthTokenResponse.TokenData("access", "refresh"), false); // success=false
        when(oauthService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.just(tokenResponse));

        service.authorizePermissionRequest(callback);

        ArgumentCaptor<SimpleEvent> captor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox, times(2)).commit(captor.capture());

        assertThat(captor.getAllValues().get(1).status()).isEqualTo(PermissionProcessStatus.INVALID);
    }

    @Test
    void authorizePermissionRequestWhenTokenExchangeFailsWithExceptionShouldCommitInvalid() throws Exception {
        OAuthCallback callback = new OAuthCallback(Optional.of("auth-code"), Optional.empty(), PERMISSION_ID);
        when(repository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permissionRequest));
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.VALIDATED);

        when(configuration.oauth()).thenReturn(oauthConfig);
        when(oauthService.exchangeCodeForToken("auth-code", "test-client"))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        service.authorizePermissionRequest(callback);

        ArgumentCaptor<SimpleEvent> captor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox, times(2)).commit(captor.capture());

        assertThat(captor.getAllValues().get(1).status()).isEqualTo(PermissionProcessStatus.INVALID);
    }
}
