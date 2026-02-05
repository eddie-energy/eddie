package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.de.eta.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.de.eta.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.de.eta.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.MalformedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata.REGION_CONNECTOR_ID;

/**
 * Service for creating permission requests for the German (DE) ETA Plus region connector.
 */
@Service
public class PermissionRequestCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestCreationService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final Outbox outbox;

    public PermissionRequestCreationService(
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            Outbox outbox
    ) {
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.outbox = outbox;
    }

    /**
     * Creates a new permission request.
     *
     * @param requestForCreation the request creation DTO
     * @return the created permission request
     * @throws DataNeedNotFoundException if the data need is not found
     * @throws UnsupportedDataNeedException if the data need is not supported
     */
    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation requestForCreation)
            throws DataNeedNotFoundException, UnsupportedDataNeedException {
        String permissionId = UUID.randomUUID().toString();
        String dataNeedId = requestForCreation.dataNeedId();

        LOGGER.info("Creating new permission request {} for metering point {}", 
                permissionId, requestForCreation.meteringPointId());

        outbox.commit(new CreatedEvent(
                permissionId,
                dataNeedId,
                requestForCreation.connectionId(),
                requestForCreation.meteringPointId()
        ));

        switch (dataNeedCalculationService.calculate(dataNeedId)) {
            case AccountingPointDataNeedResult ignored -> {
                String message = "AccountingPointDataNeed not supported by DE-ETA connector";
                outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
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

            case ValidatedHistoricalDataDataNeedResult(
                    List<Granularity> granularities,
                    Timeframe ignored,
                    Timeframe energyTimeframe
            ) -> {
                LOGGER.info("Validated permission request {}", permissionId);
                outbox.commit(new ValidatedEvent(
                        permissionId,
                        energyTimeframe.start(),
                        energyTimeframe.end(),
                        granularities.getFirst()
                ));
                return new CreatedPermissionRequest(permissionId);
            }
        }
    }
}
