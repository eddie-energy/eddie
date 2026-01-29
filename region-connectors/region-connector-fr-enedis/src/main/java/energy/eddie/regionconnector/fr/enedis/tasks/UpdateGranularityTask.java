// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.tasks;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisApiError;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrGranularityUpdateEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class UpdateGranularityTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateGranularityTask.class);
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> calculationService;

    public UpdateGranularityTask(Outbox outbox, DataNeedCalculationService<DataNeed> calculationService) {
        this.outbox = outbox;
        this.calculationService = calculationService;
    }

    public void update(FrEnedisPermissionRequest permissionRequest, Throwable throwable) {
        var permissionId = permissionRequest.permissionId();
        if (!isDataNotFoundError(throwable)) {
            return;
        }
        if (containsHigherGranularity(permissionRequest)) {
            LOGGER.info("Updating granularity of permission request {} from {} to {}",
                        permissionId,
                        permissionRequest.granularity(),
                        Granularity.P1D);
            outbox.commit(new FrGranularityUpdateEvent(permissionId, Granularity.P1D));
            return;
        }
        LOGGER.info(
                "Could not update permission request {} to use higher granularity, since it already uses highest possible granularity {}",
                permissionId,
                Granularity.P1D
        );
        outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
    }

    private static boolean isDataNotFoundError(Throwable throwable) {
        if (!(throwable instanceof WebClientResponseException webclientException)) {
            return false;
        }
        var error = webclientException.getResponseBodyAs(EnedisApiError.class);
        return error != null && "no_data_found".equals(error.error());
    }

    private boolean containsHigherGranularity(FrEnedisPermissionRequest permissionRequest) {
        if (permissionRequest.granularity() == Granularity.P1D) {
            return false;
        }
        var result = calculationService.calculate(permissionRequest.dataNeedId(), permissionRequest.created());
        return switch (result) {
            case ValidatedHistoricalDataDataNeedResult vhd when vhd.granularities().contains(Granularity.P1D) -> true;
            default -> false;
        };
    }
}
