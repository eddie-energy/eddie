// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.be.fluvius.permission.events.CreatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.MalformedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionRequestService {
    public static final String DATA_NEED_ID = "dataNeedId";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final Outbox outbox;

    public PermissionRequestService(DataNeedCalculationService<DataNeed> calculationService, Outbox outbox) {
        this.calculationService = calculationService;
        this.outbox = outbox;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation)
            throws UnsupportedDataNeedException, DataNeedNotFoundException {
        String permissionId = UUID.randomUUID().toString();
        var dataNeedId = permissionRequestForCreation.dataNeedId();
        outbox.commit(new CreatedEvent(permissionId,
                                       dataNeedId,
                                       permissionRequestForCreation.connectionId()));

        var calculation = calculationService.calculate(dataNeedId);
        switch (calculation) {
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new MalformedEvent(permissionId,
                                                 new AttributeError(DATA_NEED_ID, "DataNeed not found!")));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
            }
            case ValidatedHistoricalDataDataNeedResult(
                    List<Granularity> granularities,
                    Timeframe ignored,
                    Timeframe energyTimeframe
            ) -> {
                LOGGER.info("Created permission request {}", permissionId);
                outbox.commit(new ValidatedEvent(permissionId,
                                                 energyTimeframe.start(),
                                                 energyTimeframe.end(),
                                                 granularities.getFirst(),
                                                 permissionRequestForCreation.flow()));
                return new CreatedPermissionRequest(permissionId);
            }
            default -> {
                var message = calculation.getClass().getSimpleName() + " not supported";
                outbox.commit(new MalformedEvent(permissionId, List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
        }
    }
}
