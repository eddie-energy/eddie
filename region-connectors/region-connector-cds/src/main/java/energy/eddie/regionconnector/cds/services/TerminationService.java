package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.revocation.RevocationResult;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private final CdsServerRepository cdsServerRepository;
    private final CdsPublicApis cdsPublicApis;
    private final OAuthCredentialsRepository oAuthCredentialsRepository;
    private final OAuthService oAuthService;
    private final Outbox outbox;

    public TerminationService(
            CdsServerRepository cdsServerRepository,
            CdsPublicApis cdsPublicApis,
            OAuthCredentialsRepository credentialsRepository,
            OAuthService oAuthService,
            Outbox outbox
    ) {
        this.cdsServerRepository = cdsServerRepository;
        this.cdsPublicApis = cdsPublicApis;
        this.oAuthCredentialsRepository = credentialsRepository;
        this.oAuthService = oAuthService;
        this.outbox = outbox;
    }

    public void terminate(CdsPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Externally terminating permission request {}", permissionId);
        var cdsServer = cdsServerRepository.getReferenceById(permissionRequest.dataSourceInformation().cdsServerId());
        var credentials = oAuthCredentialsRepository.getOAuthCredentialByPermissionId(permissionId);
        cdsPublicApis.carbonDataSpec(URI.create(cdsServer.baseUri()))
                     .flatMap(res -> cdsPublicApis.oauthMetadataSpec(res.getOauthMetadata()))
                     .map(res -> oAuthService.revokeToken(res.getRevocationEndpoint(), cdsServer, credentials))
                     .subscribe(result -> handleResult(permissionRequest, result, cdsServer));
    }

    private void handleResult(CdsPermissionRequest permissionRequest, RevocationResult result, CdsServer cdsServer) {
        var permissionId = permissionRequest.permissionId();
        switch (result) {
            case RevocationResult.InvalidRevocationRequest invalidRequest -> {
                LOGGER.warn("Error while revoking token {} for permission request {}",
                            invalidRequest.reason(),
                            permissionId);
                var status = invalidRequest.isUnsupportedTokenType()
                        ? PermissionProcessStatus.EXTERNALLY_TERMINATED
                        : PermissionProcessStatus.FAILED_TO_TERMINATE;
                outbox.commit(new SimpleEvent(permissionId, status));
            }
            case RevocationResult.ServiceUnavailable ignored -> {
                LOGGER.atInfo()
                      .addArgument(cdsServer::baseUri)
                      .addArgument(permissionId)
                      .log("CDS Server {} is currently unavailable to terminate permission request {}");
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FAILED_TO_TERMINATE));
            }
            case RevocationResult.SuccessfulRevocation ignored -> {
                LOGGER.info("Successfully externally terminated permission request {}", permissionId);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.EXTERNALLY_TERMINATED));
            }
        }
    }
}
