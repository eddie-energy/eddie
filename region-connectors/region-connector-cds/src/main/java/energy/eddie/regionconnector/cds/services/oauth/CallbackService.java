package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.cds.services.oauth.authorization.AcceptedResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.AuthorizationResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.ErrorResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.UnauthorizedResult;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class CallbackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackService.class);
    private final Outbox outbox;
    private final CdsPermissionRequestRepository permissionRequestRepository;
    private final OAuthService oAuthService;
    private final CdsServerRepository cdsServerRepository;
    private final OAuthCredentialsRepository credentialsRepository;

    public CallbackService(
            Outbox outbox,
            CdsPermissionRequestRepository permissionRequestRepository,
            OAuthService oAuthService,
            CdsServerRepository cdsServerRepository,
            OAuthCredentialsRepository credentialsRepository
    ) {
        this.outbox = outbox;
        this.permissionRequestRepository = permissionRequestRepository;
        this.oAuthService = oAuthService;
        this.cdsServerRepository = cdsServerRepository;
        this.credentialsRepository = credentialsRepository;
    }

    public AuthorizationResult processCallback(Callback callback) throws PermissionNotFoundException {
        var pr = permissionRequestRepository.findByState(callback.state())
                                            .orElseThrow(() -> new PermissionNotFoundException(callback.state()));
        var permissionId = pr.permissionId();

        if (pr.status() == PermissionProcessStatus.REJECTED) {
            LOGGER.info("Permission request {} was already rejected", permissionId);
            return new UnauthorizedResult(permissionId, pr.status());
        }
        if (pr.status() == PermissionProcessStatus.ACCEPTED) {
            LOGGER.info("Permission request {} was already accepted", permissionId);
            return new AcceptedResult(permissionId, pr.dataNeedId());
        }
        if (pr.status() != PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR) {
            LOGGER.info("Operation cannot be done for permission request {} in state {}", permissionId, pr.status());
            return new ErrorResult(permissionId, "Wrong status of permission request " + pr.status());
        }
        var error = callback.error();
        if (error != null) {
            LOGGER.info("Permission request {} had error present {}", permissionId, error);
            if (error.equals("access_denied")) {
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
                return new UnauthorizedResult(permissionId, PermissionProcessStatus.REJECTED);
            } else {
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
                return new ErrorResult(permissionId, error);
            }
        }
        var code = callback.code();
        if (code == null) {
            LOGGER.info("Permission request {} has no code", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return new ErrorResult(permissionId, "No code provided");
        }

        var cdsServerId = pr.dataSourceInformation().cdsServerId();
        return getToken(code, permissionId, pr, cdsServerId);
    }

    private AuthorizationResult getToken(
            String code,
            String permissionId,
            CdsPermissionRequest pr,
            Long cdsServerId
    ) {
        var cdsServer = cdsServerRepository.getReferenceById(cdsServerId);
        var result = oAuthService.retrieveAccessToken(code, cdsServer);
        return switch (result) {
            case CredentialsWithRefreshToken(String accessToken, String refreshToken, ZonedDateTime expiresAt) -> {
                credentialsRepository.save(new OAuthCredentials(permissionId, refreshToken, accessToken, expiresAt));
                outbox.commit(new AcceptedEvent(permissionId));
                yield new AcceptedResult(permissionId, pr.dataNeedId());
            }
            case InvalidTokenResult ignored -> {
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
                yield new ErrorResult(permissionId, "Could not retrieve access token");
            }
            case CredentialsWithoutRefreshToken(String accessToken, ZonedDateTime expiresAt) -> {
                credentialsRepository.save(new OAuthCredentials(permissionId, null, accessToken, expiresAt));
                outbox.commit(new AcceptedEvent(permissionId));
                yield new AcceptedResult(permissionId, pr.dataNeedId());
            }
        };
    }
}
