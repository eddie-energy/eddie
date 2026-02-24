// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fi.fingrid.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fi.fingrid.permission.events.*;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCreationService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final FiPermissionRequestRepository permissionRequestRepository;

    public PermissionCreationService(
            Outbox outbox,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            FiPermissionRequestRepository permissionRequestRepository
    ) {
        this.outbox = outbox;
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public CreatedPermissionRequest createAndValidatePermissionRequest(PermissionRequestForCreation permissionRequest) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        String permissionId = UUID.randomUUID().toString();
        LOGGER.info("Creating permission request with id {}", permissionId);
        var dataNeedId = permissionRequest.dataNeedId();
        outbox.commit(new CreatedEvent(permissionId,
                                       permissionRequest.connectionId(),
                                       dataNeedId,
                                       permissionRequest.customerIdentification()
        ));
        var calculation = dataNeedCalculationService.calculate(dataNeedId);
        var validatedEvent = switch (calculation) {
            case AiidaDataNeedResult ignored -> {
                String message = "AiidaDataNeedResult not supported!";
                outbox.commit(new MalformedEvent(permissionId, List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new MalformedEvent(
                        permissionId,
                        List.of(new AttributeError(DATA_NEED_ID, "Unknown dataNeedId"))
                ));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new MalformedEvent(permissionId, List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
            case AccountingPointDataNeedResult(Timeframe permissionTimeframe) -> new ValidatedEvent(
                    permissionId,
                    permissionTimeframe.start(),
                    permissionTimeframe.end()
            );
            case ValidatedHistoricalDataDataNeedResult vhdResult -> new ValidatedEvent(
                    permissionId,
                    vhdResult.granularities().getFirst(),
                    vhdResult.energyTimeframe().start(),
                    vhdResult.energyTimeframe().end()
            );
        };

        outbox.commit(validatedEvent);
        return new CreatedPermissionRequest(permissionId);
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void acceptOrReject(
            String permissionId,
            PermissionProcessStatus status
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        if (status != PermissionProcessStatus.ACCEPTED && status != PermissionProcessStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid permission status: " + status);
        }
        var pr = permissionRequestRepository.findByPermissionId(permissionId)
                                            .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        if (pr.status() != PermissionProcessStatus.VALIDATED) {
            throw new PermissionStateTransitionException(permissionId,
                                                         PermissionProcessStatus.VALIDATED,
                                                         PermissionProcessStatus.VALIDATED,
                                                         pr.status());
        }
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        if (status == PermissionProcessStatus.REJECTED) {
            outbox.commit(new SimpleEvent(permissionId, status));
        } else {
            outbox.commit(new AcceptedEvent(permissionId));
        }
    }
}
