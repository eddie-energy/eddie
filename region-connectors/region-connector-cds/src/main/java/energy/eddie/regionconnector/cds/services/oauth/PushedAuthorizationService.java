package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientFactory;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.OAuthRequestType;
import energy.eddie.regionconnector.cds.services.oauth.par.ErrorParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "region-connector.cds.par.enabled", havingValue = "true")
public class PushedAuthorizationService implements AuthorizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushedAuthorizationService.class);
    private final OAuthService oAuthService;
    private final Outbox outbox;
    private final CustomerDataClientFactory factory;

    public PushedAuthorizationService(OAuthService oAuthService, Outbox outbox, CustomerDataClientFactory factory) {
        this.oAuthService = oAuthService;
        this.outbox = outbox;
        this.factory = factory;
    }

    @Override
    public Mono<URI> createOAuthRequest(CdsServer cdsServer, String permissionId) {
        return factory.create(cdsServer.id())
                      .mapNotNull(creds -> {
                          var res = oAuthService.pushAuthorization(cdsServer, creds, List.of(Scopes.USAGE_DETAILED_SCOPE));
                          return switch (res) {
                              case ErrorParResponse(String code) -> {
                                  LOGGER.info("Got error when requesting PAR '{}' for permission request {}",
                                              code,
                                              permissionId);
                                  outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                                  yield null;
                              }
                              case UnableToSendPar ignored -> {
                                  LOGGER.info("Was not able to send permission request {}", permissionId);
                                  outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                                  yield null;
                              }
                              case SuccessfulParResponse(URI redirectUri, ZonedDateTime expiresAt, String state) -> {
                                  outbox.commit(new SentToPaEvent(permissionId,
                                                                  expiresAt,
                                                                  state,
                                                                  OAuthRequestType.PUSHED_AUTHORIZATION_REQUEST));
                                  yield redirectUri;
                              }
                          };
                      });
    }
}
