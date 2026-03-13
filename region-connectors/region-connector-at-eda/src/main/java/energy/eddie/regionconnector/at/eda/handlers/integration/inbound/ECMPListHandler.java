// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.permission.request.events.UpdateEndDateEvent;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ECMPListHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ECMPListHandler.class);
    private final Outbox outbox;

    public ECMPListHandler(Outbox outbox, IdentifiableStreams streams) {
        this.outbox = outbox;
        streams.ecmpListStream()
               .subscribe(this::handle);
    }

    private void handle(IdentifiableECMPList identifiableECMPList) {
        var pr = identifiableECMPList.permissionRequest();
        var meteringPoint = pr.meteringPointId();
        var ecmpList = identifiableECMPList.ecmpList();
        var permissionId = pr.permissionId();
        var messageId = ecmpList.messageId();
        LOGGER.debug("Trying to update end date of permission request {} with ECMPList with message ID '{}'",
                     permissionId,
                     messageId);
        if (meteringPoint.isEmpty()) {
            LOGGER.warn(
                    "Permission request {} does not have a metering point for ECMPList with message ID '{}', but the metering point HAS to be present for the Energy Community Data Need",
                    permissionId,
                    messageId
            );
            return;
        }
        var end = identifiableECMPList.ecmpList().endDate(meteringPoint.get());

        if (end.isPresent()) {
            var newEnd = end.get().toLocalDate();
            LOGGER.debug("Updating permission request's {} end date from '{}' to '{}'", permissionId, pr.end(), newEnd);
            outbox.commit(new UpdateEndDateEvent(permissionId, newEnd));
        } else {
            LOGGER.warn(
                    "Could not update permission request's {} end date, since ECMPList document with message ID '{}' does not contain any metering points associated with the permission request",
                    permissionId,
                    messageId
            );
        }
    }
}
