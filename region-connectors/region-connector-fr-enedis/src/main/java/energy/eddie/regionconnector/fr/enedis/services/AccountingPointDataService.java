package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.contract.UsagePointContract;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrUsagePointTypeEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@Service
public class AccountingPointDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataService.class);
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(AccountingPointDataService::isRetryable);
    private final EnedisApi enedisApi;
    private final Outbox outbox;

    public AccountingPointDataService(EnedisApi enedisApi, Outbox outbox) {
        this.enedisApi = enedisApi;
        this.outbox = outbox;
    }

    /**
     * Retries when the error is: - a 429 TooManyRequests - a 401 Unauthorized (i.e. when the token is expired)
     */
    private static boolean isRetryable(Throwable e) {
        var retryable = e instanceof WebClientResponseException.TooManyRequests || e instanceof WebClientResponseException.Unauthorized;
        LOGGER.info("Checking if error is retryable({})", retryable, e);
        return retryable;
    }

    public void fetchMeteringPointSegment(String permissionId, String usagePointId) {
        LOGGER.info("Fetching metering point segment for permissionId '{}'", permissionId);
        enedisApi.getContract(usagePointId)
                 .retryWhen(RETRY_BACKOFF_SPEC)
                 .doOnError(e -> handleError(permissionId, e))
                 .onErrorComplete()
                 .subscribe(contract -> handleMeteringPointSegment(permissionId, contract));
    }

    private void handleError(String permissionId, Throwable e) {
        LOGGER.error("Error while fetching contract data from ENEDIS for permissionId '{}'", permissionId, e);
        if (e instanceof WebClientResponseException.Forbidden) {
            LOGGER.warn("Revoking permission request for permissionId '{}'", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
        }
    }

    private void handleMeteringPointSegment(String permissionId, CustomerContract contract) {
        LOGGER.info("Received contract data for permissionId '{}'", permissionId);
        if (contract.usagePointContracts().isEmpty()) {
            LOGGER.warn("No usage point contracts found for permissionId '{}'", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return;
        }

        boolean consumption = false;
        boolean production = false;

        for (UsagePointContract usagePointContract : contract.usagePointContracts()) {
            var segment = usagePointContract.contract().segment();
            if (Strings.isEmpty(segment)) {
                continue;
            }

            if (segment.contains("C")) {
                consumption = true;
            }
            if (segment.contains("P")) {
                production = true;
            }
        }

        var usagePointType = UsagePointType.fromBooleans(consumption, production);
        if (usagePointType.isEmpty()) {
            LOGGER.warn("MeteringPoint of permission request '{}' is neither consumption nor production", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return;
        }

        outbox.commit(new FrUsagePointTypeEvent(permissionId, usagePointType.get()));
    }
}
