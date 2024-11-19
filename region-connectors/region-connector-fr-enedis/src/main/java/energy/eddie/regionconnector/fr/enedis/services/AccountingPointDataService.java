package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisAccountingPointDataApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrUsagePointTypeEvent;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.CommonAccountingPointDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@Service
public class AccountingPointDataService implements CommonAccountingPointDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataService.class);
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(AccountingPointDataService::isRetryable);
    private final EnedisAccountingPointDataApi enedisApi;
    private final Sinks.Many<IdentifiableAccountingPointData> sink;

    private final Outbox outbox;

    public AccountingPointDataService(
            EnedisAccountingPointDataApi enedisApi,
            Sinks.Many<IdentifiableAccountingPointData> sink,
            Outbox outbox
    ) {
        this.enedisApi = enedisApi;
        this.sink = sink;
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

    public void fetchAccountingPointData(MeterReadingPermissionRequest request, String usagePointId) {
        LOGGER.atInfo()
              .addArgument(request::permissionId)
              .log("Fetching accounting point data for permissionId '{}'");

        Mono<CustomerContract> contractMono = enedisApi.getContract(usagePointId).retryWhen(RETRY_BACKOFF_SPEC);
        Mono<CustomerAddress> addressMono = enedisApi.getAddress(usagePointId).retryWhen(RETRY_BACKOFF_SPEC);
        Mono<CustomerIdentity> identityMono = enedisApi.getIdentity(usagePointId).retryWhen(RETRY_BACKOFF_SPEC);
        Mono<CustomerContact> contactMono = enedisApi.getContact(usagePointId).retryWhen(RETRY_BACKOFF_SPEC);

        Mono.zip(contractMono, addressMono, identityMono, contactMono)
            .subscribe(
                    tuple -> handleAccountingPointData(
                            (FrEnedisPermissionRequest) request,
                            new IdentifiableAccountingPointData(
                                    (FrEnedisPermissionRequest) request, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()
                            )
                    ),
                    e -> handleError(request.permissionId(), e)
            );
    }

    private void handleAccountingPointData(
            FrEnedisPermissionRequest permissionRequest,
            IdentifiableAccountingPointData identifiableAccountingPointData
    ) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Received accounting point data for permissionId '{}'");
        sink.tryEmitNext(identifiableAccountingPointData);
        outbox.commit(new FrSimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.FULFILLED));
    }

    private void handleError(String permissionId, Throwable e) {
        LOGGER.error("Error while fetching contract data from ENEDIS for permissionId '{}'", permissionId, e);
        if (e instanceof WebClientResponseException.Forbidden) {
            // When we receive a FORBIDDEN, it either means the customer revoked the permission, or that the given client credentials to not have the necessary scopes set (at ENEDIS).
            // Since we fetch accounting point data immediately, the customer should not have enough time to revoke a permission,
            // so we assume 403 implies, that the client credentials do not have the necessary scope set and the PermissionRequest is thus UNFULFILLABLE.
            LOGGER.warn(
                    "Permission request for permissionId '{}' is unfulfillable. Verify that the provided Enedis Client Credentials have the necessary scopes set up.",
                    permissionId
            );
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
        }
    }

    public void fetchMeteringPointSegment(String permissionId, String usagePointId) {
        LOGGER.info("Fetching metering point segment for permissionId '{}'", permissionId);
        enedisApi.getContract(usagePointId)
                 .retryWhen(RETRY_BACKOFF_SPEC)
                 .doOnError(e -> handleError(permissionId, e))
                 .onErrorComplete()
                 .subscribe(contract -> handleMeteringPointSegment(permissionId, contract));
    }

    private void handleMeteringPointSegment(String permissionId, CustomerContract contract) {
        LOGGER.info("Received contract data for permissionId '{}'", permissionId);
        if (contract.usagePointContracts().isEmpty()) {
            LOGGER.warn("No usage point contracts found for permissionId '{}'", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return;
        }

        var usagePointType = UsagePointType.fromCustomerContract(contract);
        if (usagePointType.isEmpty()) {
            LOGGER.warn("MeteringPoint of permission request '{}' is neither consumption nor production", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return;
        }

        outbox.commit(new FrUsagePointTypeEvent(permissionId, usagePointType.get()));
    }
}
