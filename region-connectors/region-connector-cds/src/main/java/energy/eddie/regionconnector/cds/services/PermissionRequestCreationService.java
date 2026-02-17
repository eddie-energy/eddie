// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.cds.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.cds.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.permission.events.CreatedEvent;
import energy.eddie.regionconnector.cds.permission.events.MalformedEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.AuthorizationService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionRequestCreationService {
    public static final String DATA_NEED_FIELD = "dataNeedId";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestCreationService.class);
    private final CdsServerRepository cdsServerRepository;
    private final Outbox outbox;
    private final CdsServerCalculationService calculationService;
    private final AuthorizationService authorizationService;

    public PermissionRequestCreationService(
            CdsServerRepository cdsServerRepository, Outbox outbox,
            CdsServerCalculationService calculationService,
            AuthorizationService authorizationService
    ) {
        this.cdsServerRepository = cdsServerRepository;
        this.outbox = outbox;
        this.calculationService = calculationService;
        this.authorizationService = authorizationService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation creation) throws UnknownPermissionAdministratorException, UnsupportedDataNeedException, DataNeedNotFoundException {
        var permissionId = UUID.randomUUID().toString();
        var cdsServerId = creation.cdsId();
        LOGGER.info("Creating new permission request {} for cds server with id {}", permissionId, cdsServerId);
        var dataNeedId = creation.dataNeedId();
        var createdEvent = new CreatedEvent(permissionId,
                                            creation.connectionId(),
                                            dataNeedId,
                                            cdsServerId);
        outbox.commit(createdEvent);
        var cdsServer = cdsServerRepository.findById(cdsServerId);
        if (cdsServer.isEmpty()) {
            LOGGER.info("Malformed permission request {} for unknown CDS server {}", permissionId, cdsServerId);
            outbox.commit(new MalformedEvent(permissionId,
                                             new AttributeError("cdsId", "Unknown permission administrator")));
            throw new UnknownPermissionAdministratorException(cdsServerId);
        }
        var calc = calculationService.calculate(dataNeedId, cdsServer.get(), createdEvent.eventCreated());
        switch (calc) {
            case AiidaDataDataNeedResult ignored -> {
                String message = "AiidaDataDataNeedResult not supported!";
                outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_FIELD, message)));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
            case DataNeedNotFoundResult ignored -> {
                LOGGER.info("Data need {} not found", dataNeedId);
                outbox.commit(new MalformedEvent(permissionId,
                                                 new AttributeError(DATA_NEED_FIELD, "Data need not found")));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                LOGGER.info("Data need {} not supported '{}'", dataNeedId, message);
                throw unsupportedDataNeed(permissionId, message, dataNeedId);
            }
            case AccountingPointDataNeedResult ignored ->
                    outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.VALIDATED));
            case ValidatedHistoricalDataDataNeedResult vhdResult -> outbox
                    .commit(new ValidatedEvent(permissionId,
                                               vhdResult.granularities().getFirst(),
                                               vhdResult.energyTimeframe().start(),
                                               vhdResult.energyTimeframe().end()));
        }
        var uri = authorizationService.createOAuthRequest(cdsServer.get(), permissionId);
        return new CreatedPermissionRequest(permissionId, uri);
    }

    private UnsupportedDataNeedException unsupportedDataNeed(String permissionId, String message, String dataNeedId) {
        outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_FIELD, message)));
        return new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
    }
}
