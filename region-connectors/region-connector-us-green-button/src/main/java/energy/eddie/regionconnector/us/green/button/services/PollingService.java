package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.exceptions.DataNotReadyException;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.permission.events.UsPollingNotReadyEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class PollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final UsPermissionRequestRepository permissionRequestRepository;
    private final GreenButtonApi api;
    private final DataNeedsService dataNeedsService;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final CredentialService credentialService;
    private final PublishService publishService;
    private final Outbox outbox;

    public PollingService(
            UsPermissionRequestRepository permissionRequestRepository,
            GreenButtonApi api,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService,
            DataNeedCalculationService<DataNeed> calculationService,
            CredentialService credentialService,
            PublishService publishService,
            Outbox outbox
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.api = api;
        this.dataNeedsService = dataNeedsService;
        this.calculationService = calculationService;
        this.credentialService = credentialService;
        this.publishService = publishService;
        this.outbox = outbox;
    }


    // To force Hibernate to discard the first level cache
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void poll(String permissionId) {
        LOGGER.info("Polling for permission request {}", permissionId);
        var permissionRequest = permissionRequestRepository.getByPermissionId(permissionId);
        if (permissionRequest.start().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            LOGGER.info("Permission request {} has not started yet", permissionId);
            return;
        }
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = dataNeedsService.getById(dataNeedId);
        var calc = calculationService.calculate(dataNeed);
        if (!calc.supportsDataNeed()) {
            LOGGER.warn("DataNeed {} not supported for permission request {}", dataNeedId, permissionId);
            return;
        }
        credentialService.retrieveAccessToken(permissionRequest)
                         .subscribe(credentials -> {
                                        if (dataNeed instanceof ValidatedHistoricalDataDataNeed)
                                            pollValidatedHistoricalData(permissionRequest, credentials, calc);
                                    },
                                    throwable -> handleAccessTokenError(throwable, permissionId));
    }

    private void pollValidatedHistoricalData(
            UsGreenButtonPermissionRequest permissionRequest,
            OAuthTokenDetails creds,
            DataNeedCalculation calc
    ) {
        if (calc.energyDataTimeframe() == null) {
            return;
        }
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);
        var start = calc.energyDataTimeframe().start().atStartOfDay(ZoneOffset.UTC);
        var energyDataStart = permissionRequest.latestMeterReadingEndDateTime()
                                               .orElse(start);
        var end = DateTimeUtils.endOfDay(calc.energyDataTimeframe().end(), ZoneOffset.UTC);
        var now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
        var energyDataEnd = end.isBefore(now) ? end : now;
        api.batchSubscription(creds.authUid(), creds.accessToken(), energyDataStart, energyDataEnd)
           .map(res -> new IdentifiableSyndFeed(permissionRequest, res))
           .subscribe(publishService::publish,
                      throwable -> handlePollingError(throwable, permissionId));
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
            case DataNotReadyException ignored -> {
                LOGGER.info("Data not ready for permission request {}", permissionId);
                outbox.commit(new UsPollingNotReadyEvent(permissionId));
            }
            default -> LOGGER.warn("Got unexpected exception for permission {}", permissionId, throwable);
        }
    }

    private void onForbidden(String permissionId, Throwable throwable) {
        LOGGER.info("Got forbidden on poll of {}, revoking permission request", permissionId, throwable);
        outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
    }
}
