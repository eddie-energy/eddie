// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.MalformedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEventFactory;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class PermissionRequestCreationAndValidationService {
    private static final String DATA_NEED_ID = "dataNeedId";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestCreationAndValidationService.class);
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final ValidatedEventFactory validatedEventFactory;

    public PermissionRequestCreationAndValidationService(
            Outbox outbox,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            ValidatedEventFactory validatedEventFactory
    ) {
        this.outbox = outbox;
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.validatedEventFactory = validatedEventFactory;
    }

    /**
     * Creates and validates a number permission requests.
     * This will emit a <code>CreatedEvent</code>, and a <code>ValidatedEvent</code> or a <code>MalformedEvent</code> for each created permission request.
     * If only one data need ID is present, any exceptions that occur will be forwarded.
     * That way callers, such as the controller, can react accordingly.
     * If multiple data need IDs are present, the exceptions will be logged and only valid permission IDs are returned to the frontend.
     *
     * @param permissionRequest the DTO that is the base for the created and validated permission requests.
     * @return a DTO with the ids of the created and validated permission requests
     * @throws DataNeedNotFoundException    If the data need ID is not found, will only be thrown if exactly one data need ID is present
     * @throws UnsupportedDataNeedException If the data need ID is not supported, will only be thrown if exactly one data need ID is present
     */
    public CreatedPermissionRequest createAndValidatePermissionRequest(PermissionRequestForCreation permissionRequest) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var dataNeeds = permissionRequest.dataNeedIds();
        var permissionIds = new ArrayList<String>();
        if (dataNeeds.size() == 1) {
            var permissionId = createPermissionRequest(permissionRequest, dataNeeds.getFirst());
            return new CreatedPermissionRequest(List.of(permissionId));
        }
        for (var dataNeedId : dataNeeds) {
            try {
                var permissionId = createPermissionRequest(permissionRequest, dataNeedId);
                permissionIds.add(permissionId);
            } catch (Exception e) {
                LOGGER.info("Got exception while creating permission request for dataNeedId: {}", dataNeedId, e);
            }
        }
        return new CreatedPermissionRequest(permissionIds);
    }

    private String createPermissionRequest(
            PermissionRequestForCreation permissionRequest,
            String dataNeedId
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        var created = ZonedDateTime.now(AT_ZONE_ID);

        var createdEvent = new CreatedEvent(
                permissionId,
                permissionRequest.connectionId(),
                dataNeedId,
                created,
                new EdaDataSourceInformation(permissionRequest.dsoId()),
                permissionRequest.meteringPointId()
        );
        outbox.commit(createdEvent);

        var calculation = dataNeedCalculationService.calculate(dataNeedId);
        var event = switch (calculation) {
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new MalformedEvent(
                        permissionId,
                        new AttributeError(DATA_NEED_ID, "Unknown DataNeed"))
                );
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
            case ValidatedHistoricalDataDataNeedResult validatedHistoricalDataDataNeedResult ->
                    validateHistoricalValidatedEvent(
                            permissionId, validatedHistoricalDataDataNeedResult
                    );
            case AccountingPointDataNeedResult ignored -> validatedEventFactory.createValidatedEvent(
                    permissionId, LocalDate.now(AT_ZONE_ID), null, null
            );
        };
        outbox.commit(event);
        return permissionId;
    }

    private ValidatedEvent validateHistoricalValidatedEvent(
            String permissionId,
            ValidatedHistoricalDataDataNeedResult calculation
    ) {
        var granularity = calculation.granularities().getFirst();
        return validatedEventFactory.createValidatedEvent(
                permissionId,
                calculation.energyTimeframe().start(),
                calculation.energyTimeframe().end(),
                AllowedGranularity.valueOf(granularity.name())
        );
    }
}
