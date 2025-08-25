package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class PollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final UsPermissionRequestRepository permissionRequestRepository;
    private final GreenButtonApi api;
    private final CredentialService credentialService;
    private final PublishService publishService;
    private final Outbox outbox;

    public PollingService(
            UsPermissionRequestRepository permissionRequestRepository,
            GreenButtonApi api,
            CredentialService credentialService,
            PublishService publishService,
            Outbox outbox
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.api = api;
        this.credentialService = credentialService;
        this.publishService = publishService;
        this.outbox = outbox;
    }

    // To force Hibernate to discard the first level cache
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void pollValidatedHistoricalData(String permissionId) {
        LOGGER.info("Polling for permission request {}", permissionId);
        var permissionRequest = permissionRequestRepository.getByPermissionId(permissionId);
        if (permissionRequest.start().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            LOGGER.info("Permission request {} has not started yet", permissionId);
            return;
        }
        var start = getStart(permissionRequest);
        var end = getEnd(permissionRequest);
        credentialService.retrieveAccessToken(permissionRequest)
                         .doOnError(throwable -> handleAccessTokenError(throwable, permissionId))
                         .flatMapMany(credentials -> pollValidatedHistoricalData(permissionRequest,
                                                                                 start,
                                                                                 end,
                                                                                 credentials))
                         .subscribe();
    }

    public Flux<IdentifiableSyndFeed> forcePollValidatedHistoricalData(
            UsGreenButtonPermissionRequest permissionRequest,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Force polling for permission request {}", permissionId);
        return credentialService.retrieveAccessToken(permissionRequest)
                                .flatMapMany(credentials -> pollValidatedHistoricalData(
                                        permissionRequest,
                                        start,
                                        end,
                                        credentials
                                ));
    }

    public void pollAccountingPointData(UsGreenButtonPermissionRequest pr) {
        credentialService.retrieveAccessToken(pr)
                         .subscribe(credentials -> pollAccountingPointData(pr, credentials),
                                    throwable -> handleAccessTokenError(throwable, pr.permissionId()));
    }

    private void pollAccountingPointData(UsGreenButtonPermissionRequest pr, OAuthTokenDetails credentials) {
        var permissionId = pr.permissionId();
        LOGGER.info("Polling accounting point data for permission request {}", permissionId);
        api.retailCustomer(pr.authorizationUid(), credentials.accessToken())
           .map(res -> new IdentifiableSyndFeed(pr, res))
           .subscribe(publishService::publishAccountingPointData,
                      throwable -> handlePollingError(throwable, permissionId));
    }

    private Flux<IdentifiableSyndFeed> pollValidatedHistoricalData(
            UsGreenButtonPermissionRequest permissionRequest,
            ZonedDateTime start,
            ZonedDateTime end,
            OAuthTokenDetails creds
    ) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling validated historical data for permission request {} from {} to {}",
                    permissionId,
                    start,
                    end);
        return api.batchSubscription(creds.authUid(),
                                     creds.accessToken(),
                                     permissionRequest.allowedMeters(),
                                     start,
                                     end)
                  .map(res -> new IdentifiableSyndFeed(permissionRequest, res))
                  .doOnNext(publishService::publishValidatedHistoricalData)
                  .doOnError(throwable -> handlePollingError(throwable, permissionId));
    }

    private static ZonedDateTime getStart(UsGreenButtonPermissionRequest permissionRequest) {
        var start = permissionRequest.start().atStartOfDay(ZoneOffset.UTC);
        return permissionRequest.latestMeterReadingEndDateTime()
                                .orElse(start);
    }

    private static ZonedDateTime getEnd(UsGreenButtonPermissionRequest permissionRequest) {
        var end = DateTimeUtils.endOfDay(permissionRequest.end(), ZoneOffset.UTC);
        var now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
        return end.isBefore(now) ? end : now;
    }

    private void handleAccessTokenError(Throwable throwable, String permissionId) {
        if (throwable instanceof NoRefreshTokenException) {
            LOGGER.info("Permission request {} does not have an access token, therefore, unfulfillable", permissionId);
            outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
        } else if (throwable instanceof WebClientResponseException.Forbidden) {
            onForbidden(permissionId, throwable);
        } else {
            LOGGER.warn("Exception while fetching access token for permission request {} occurred",
                        permissionId,
                        throwable);
        }
    }

    private void handlePollingError(Throwable throwable, String permissionId) {
        switch (throwable) {
            case WebClientResponseException.Forbidden forbidden -> onForbidden(permissionId, forbidden);
            case WebClientResponseException exception -> LOGGER.atWarn().addArgument(permissionId)
                                                               .addArgument(() -> exception.getResponseBodyAsString(
                                                                       StandardCharsets.UTF_8))
                                                               .log("Got bad request on poll of permission request {}. \n{}",
                                                                    exception);
            default -> LOGGER.warn("Got unexpected exception for permission {}", permissionId, throwable);
        }
    }

    private void onForbidden(String permissionId, Throwable throwable) {
        LOGGER.info("Got forbidden on poll of {}, revoking permission request", permissionId, throwable);
        outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
    }
}
