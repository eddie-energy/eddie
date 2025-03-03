package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.cds.services.oauth.authorization.AcceptedResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.ErrorResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.UnauthorizedResult;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private CdsPermissionRequestRepository permissionRequestRepository;
    @Mock
    private OAuthService oAuthService;
    @Mock
    private CdsServerRepository cdsServerRepository;
    @Mock
    private OAuthCredentialsRepository credentialsRepository;
    @InjectMocks
    private CallbackService callbackService;

    @Test
    void testProcessCallback_withInvalidState_throws() {
        // Given
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.empty());
        var callback = new Callback(null, null, "state");

        // When & Then
        assertThrows(PermissionNotFoundException.class, () -> callbackService.processCallback(callback));
    }

    @Test
    void testProcessCallback_forAlreadyRejectedPermissionRequest_returnsUnauthorized() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.REJECTED)
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var callback = new Callback(null, null, "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var unauth = assertInstanceOf(UnauthorizedResult.class, res);
        assertAll(
                () -> assertEquals("pid", unauth.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REJECTED, unauth.status())
        );
    }

    @Test
    void testProcessCallback_forAlreadyAcceptedPermissionRequest_returnsAccepted() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setDataNeedId("dnid")
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var callback = new Callback(null, null, "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var accepted = assertInstanceOf(AcceptedResult.class, res);
        assertAll(
                () -> assertEquals("pid", accepted.permissionId()),
                () -> assertEquals("dnid", accepted.dataNeedId())
        );
    }

    @Test
    void testProcessCallback_forNotSentPermissionRequest_returnsErrorResult() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.REVOKED)
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var callback = new Callback(null, null, "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var error = assertInstanceOf(ErrorResult.class, res);
        assertAll(
                () -> assertEquals("pid", error.permissionId()),
                () -> assertEquals("Wrong status of permission request REVOKED", error.error())
        );
    }

    @Test
    void testProcessCallback_withAccessDeniedError_returnsUnauthorized() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var callback = new Callback(null, "access_denied", "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var error = assertInstanceOf(UnauthorizedResult.class, res);
        assertAll(
                () -> assertEquals("pid", error.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REJECTED, error.status())
        );
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REJECTED, event.status())));
    }

    @Test
    void testProcessCallback_withOtherError_returnsErrorResult() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var callback = new Callback(null, "other_error", "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var error = assertInstanceOf(ErrorResult.class, res);
        assertAll(
                () -> assertEquals("pid", error.permissionId()),
                () -> assertEquals("other_error", error.error())
        );
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.INVALID, event.status())));
    }

    @Test
    void testProcessCallback_withoutCode_returnsErrorCode() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .setCdsServer(1L)
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var callback = new Callback(null, null, "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var error = assertInstanceOf(ErrorResult.class, res);
        assertAll(
                () -> assertEquals("pid", error.permissionId()),
                () -> assertEquals("No code provided", error.error())
        );
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.INVALID, event.status())));
    }

    @Test
    void testProcessCallback_withCode_returnsAccepted() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .setCdsServer(1)
                .setDataNeedId("dnid")
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var cdsServer = new CdsServerBuilder().setBaseUri("http://localhost")
                                              .setId(1L)
                                              .setName("CDS server")
                                              .setCoverages(Set.of())
                                              .setAdminClientId("client-id")
                                              .setAdminClientSecret("client-secret")
                                              .setTokenEndpoint("http://localhost")
                                              .build();
        when(cdsServerRepository.getReferenceById(1L))
                .thenReturn(cdsServer);
        var credentials = new CredentialsWithRefreshToken("accessToken",
                                                          "refreshToken",
                                                          ZonedDateTime.now(ZoneOffset.UTC));
        when(oAuthService.retrieveAccessToken("code", cdsServer))
                .thenReturn(credentials);
        var callback = new Callback("code", null, "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var accepted = assertInstanceOf(AcceptedResult.class, res);
        assertAll(
                () -> assertEquals("pid", accepted.permissionId()),
                () -> assertEquals("dnid", accepted.dataNeedId())
        );
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.ACCEPTED, event.status())));
        verify(credentialsRepository).save(any());
    }

    @Test
    void testProcessCallback_withCodeWithoutRefreshToken_returnsAccepted() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .setCdsServer(1)
                .setDataNeedId("dnid")
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var cdsServer = new CdsServerBuilder().setBaseUri("http://localhost")
                                              .setId(1L)
                                              .setName("CDS server")
                                              .setCoverages(Set.of())
                                              .setAdminClientId("client-id")
                                              .setAdminClientSecret("client-secret")
                                              .setTokenEndpoint("http://localhost")
                                              .build();
        when(cdsServerRepository.getReferenceById(1L))
                .thenReturn(cdsServer);
        var credentials = new CredentialsWithoutRefreshToken("accessToken",
                                                             ZonedDateTime.now(ZoneOffset.UTC));
        when(oAuthService.retrieveAccessToken("code", cdsServer))
                .thenReturn(credentials);
        var callback = new Callback("code", null, "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var accepted = assertInstanceOf(AcceptedResult.class, res);
        assertAll(
                () -> assertEquals("pid", accepted.permissionId()),
                () -> assertEquals("dnid", accepted.dataNeedId())
        );
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.ACCEPTED, event.status())));
        verify(credentialsRepository).save(any());
    }


    @Test
    void testProcessCallback_withInvalidCode_returnsInvalid() throws PermissionNotFoundException {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .setCdsServer(1)
                .setDataNeedId("dnid")
                .build();
        when(permissionRequestRepository.findByState("state"))
                .thenReturn(Optional.of(pr));
        var cdsServer = new CdsServerBuilder().setBaseUri("http://localhost")
                                              .setName("CDS server")
                                              .setCoverages(Set.of())
                                              .setAdminClientId("client-id")
                                              .setAdminClientSecret("client-secret")
                                              .setTokenEndpoint("http://localhost")
                                              .build();
        when(cdsServerRepository.getReferenceById(1L))
                .thenReturn(cdsServer);
        when(oAuthService.retrieveAccessToken("code", cdsServer))
                .thenReturn(new InvalidTokenResult());
        var callback = new Callback("code", null, "state");

        // When
        var res = callbackService.processCallback(callback);

        // Then
        var error = assertInstanceOf(ErrorResult.class, res);
        assertAll(
                () -> assertEquals("pid", error.permissionId()),
                () -> assertEquals("Could not retrieve access token", error.error())
        );
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.INVALID, event.status())));
    }
}