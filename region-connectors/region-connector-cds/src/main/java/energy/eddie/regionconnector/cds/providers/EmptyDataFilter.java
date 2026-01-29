// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component
public class EmptyDataFilter implements Predicate<IdentifiableValidatedHistoricalData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmptyDataFilter.class);
    private final Outbox outbox;

    public EmptyDataFilter(Outbox outbox) {this.outbox = outbox;}

    @Override
    public boolean test(IdentifiableValidatedHistoricalData data) {
        if (!data.payload().usageSegments().isEmpty()) {
            return true;
        }
        var permissionId = data.permissionRequest().permissionId();
        LOGGER.atInfo()
              .addArgument(permissionId)
              .addArgument(() -> data.permissionRequest().dataNeedId())
              .log("Permission request {} is being marked as unfulfillable, since no usage segment contains any data related to the data need {}");
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
        return false;
    }
}
