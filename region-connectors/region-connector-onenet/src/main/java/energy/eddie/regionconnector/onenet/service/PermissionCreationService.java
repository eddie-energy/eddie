// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.service;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotFoundResult;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.onenet.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.onenet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.onenet.permission.events.CreatedEvent;
import energy.eddie.regionconnector.onenet.permission.events.SimpleEvent;
import energy.eddie.regionconnector.onenet.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static energy.eddie.regionconnector.onenet.OneNetRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionCreationService {
    private final Outbox outbox;
    private final DataNeedCalculationService calculationService;

    public PermissionCreationService(Outbox outbox, DataNeedCalculationService calculationService) {
        this.outbox = outbox;
        this.calculationService = calculationService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation creationDto) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        var createdEvent = new CreatedEvent(permissionId, creationDto.connectionId(), creationDto.dataNeedId());
        outbox.commit(createdEvent);

        // TODO: Extend logic required to create a valid connection with onenet
        switch (calculationService.calculate(createdEvent.dataNeedId(), createdEvent.eventCreated())) {
            case ValidatedHistoricalDataDataNeedResult res -> {
                // TODO: Further validation if required
                outbox.commit(new ValidatedEvent(permissionId,
                                                 res.granularities().getFirst(),
                                                 res.energyTimeframe().start(),
                                                 res.energyTimeframe().end()));
                // TODO: Refine if needed
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.ACCEPTED));
            }
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.MALFORMED));
                throw new DataNeedNotFoundException(createdEvent.dataNeedId());
            }
            default -> {
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.MALFORMED));
                // TODO: Refine error message
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                       createdEvent.dataNeedId(),
                                                       "Unsupported data need");
            }
        }
        return new CreatedPermissionRequest(permissionId);
    }
}
