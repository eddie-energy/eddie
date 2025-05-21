package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.MalformedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEventFactory;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class PermissionRequestCreationAndValidationService {
    private static final String DATA_NEED_ID = "dataNeedId";
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
     * Creates and validates a permission request. This will emit a <code>CreatedEvent</code>, and a
     * <code>ValidatedEvent</code> or a <code>MalformedEvent</code>.
     *
     * @param permissionRequest the DTO that is the base for the created and validated permission request.
     * @return a DTO with the id of the created and validated permission request
     */
    public CreatedPermissionRequest createAndValidatePermissionRequest(
            PermissionRequestForCreation permissionRequest
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        var dataNeedId = permissionRequest.dataNeedId();
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
                throw new UnsupportedDataNeedException(EdaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
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
        return new CreatedPermissionRequest(permissionId);
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
