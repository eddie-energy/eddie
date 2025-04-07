package energy.eddie.regionconnector.shared.retransmission;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.isBeforeOrEquals;

public class RetransmissionValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetransmissionValidation.class);
    private final RegionConnectorMetadata metadata;
    private final DataNeedsService dataNeedsService;
    private final String logPrefix;

    public RetransmissionValidation(RegionConnectorMetadata metadata, DataNeedsService dataNeedsService) {
        this.metadata = metadata;
        this.dataNeedsService = dataNeedsService;
        logPrefix = metadata.id() + ":";
    }

    /**
     * Validates the permission request and retransmission request.
     * The permission request must be present and has to be either in the accepted or fulfilled state.
     * Furthermore, the retransmission request must specify a timeframe that is fully in the past and contained by the timeframe specified by the permission request.
     *
     * @param permissionRequest The permission request specified by the retransmission request.
     * @param retransmissionRequest The retransmission request that is validated with the permission request.
     * @return the validation result.
     */
    public RetransmissionResult validate(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<? extends PermissionRequest> permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        LOGGER.info("{} Validating retransmission request {}", logPrefix, retransmissionRequest);
        ZonedDateTime now = ZonedDateTime.now(metadata.timeZone());

        if (permissionRequest.isEmpty()) {
            LOGGER.warn("{} No permission with this id found: {}", logPrefix, retransmissionRequest.permissionId());
            return new PermissionRequestNotFound(
                    retransmissionRequest.permissionId(),
                    now
            );
        }

        var request = permissionRequest.get();

        if (request.status() != PermissionProcessStatus.ACCEPTED && request.status() != PermissionProcessStatus.FULFILLED) {
            LOGGER.warn("{} Can only request retransmission for accepted or fulfilled permissions, current status: {}",
                        logPrefix,
                        request.status());
            return new NoActivePermission(
                    retransmissionRequest.permissionId(),
                    now
            );
        }

        var dataNeed = dataNeedsService.getById(request.dataNeedId());
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed)) {
            var reason = "Retransmission of data for " + dataNeed.getClass()
                                                                  .getSimpleName() + " not supported";
            LOGGER.warn("{} {}", logPrefix, reason);
            return new NotSupported(
                    retransmissionRequest.permissionId(),
                    now,
                    reason
            );
        }

        var permissionId = request.permissionId();
        if (retransmissionRequest.from().isBefore(request.start()) ||
            retransmissionRequest.to().isAfter(request.end())
        ) {
            LOGGER.warn("{} Retransmission request not within permission time frame for permission request {}",
                        logPrefix,
                        permissionId);
            return new NoPermissionForTimeFrame(
                    retransmissionRequest.permissionId(),
                    now
            );
        }

        if (isBeforeOrEquals(now.toLocalDate(), retransmissionRequest.to())) {
            LOGGER.warn("{} Retransmission request to date needs to be before today!", logPrefix);
            return new NotSupported(
                    retransmissionRequest.permissionId(),
                    now,
                    "Retransmission to date needs to be before today"
            );
        }
        return new Success(permissionId, now);
    }
}
