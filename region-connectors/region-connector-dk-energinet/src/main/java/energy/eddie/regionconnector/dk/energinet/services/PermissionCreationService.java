package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.events.DKValidatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkCreatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkMalformedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.*;

@Service
public class PermissionCreationService {
    private static final String UNSUPPORTED_GRANULARITIES_MESSAGE = "This region connector does not support these granularities";
    private static final String UNSUPPORTED_DATA_NEED_MESSAGE = "This region connector only supports validated historical data data needs.";
    private static final String DATA_NEED_ID = "dataNeedId";
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;
    private final GranularityChoice granularityChoice = new GranularityChoice(SUPPORTED_GRANULARITIES);

    public PermissionCreationService(
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService
    ) {
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
    }

    /**
     * Creates a new {@link PermissionRequest}, validates it and sends it to the permission administrator.
     *
     * @param requestForCreation Dto that contains the necessary information for this permission request.
     * @return The created PermissionRequest
     */
    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation requestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        var dataNeedId = requestForCreation.dataNeedId();
        outbox.commit(new DkCreatedEvent(permissionId,
                                         requestForCreation.connectionId(),
                                         dataNeedId,
                                         requestForCreation.meteringPoint(),
                                         requestForCreation.refreshToken()));
        var referenceDate = LocalDate.now(DK_ZONE_ID);
        var dataNeed = dataNeedsService.findById(dataNeedId);
        if (dataNeed.isEmpty()) {
            outbox.commit(new DkMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID, "Unknown DataNeed"))));
            throw new DataNeedNotFoundException(dataNeedId);
        }

        if (!(dataNeed.get() instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            outbox.commit(new DkMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID,
                                                                          UNSUPPORTED_DATA_NEED_MESSAGE))));
            throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeedId,
                                                   UNSUPPORTED_DATA_NEED_MESSAGE);
        }
        DataNeedWrapper wrapper;
        try {
            wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(vhdDataNeed,
                                                                           referenceDate,
                                                                           PERIOD_EARLIEST_START,
                                                                           PERIOD_LATEST_END);
        } catch (UnsupportedDataNeedException e) {
            outbox.commit(new DkMalformedEvent(permissionId, List.of(new AttributeError(DATA_NEED_ID,
                                                                                        e.getMessage()))));
            throw e;
        }
        var granularity = granularityChoice.find(vhdDataNeed.minGranularity(), vhdDataNeed.maxGranularity());
        if (granularity == null) {
            outbox.commit(new DkMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID,
                                                                          UNSUPPORTED_GRANULARITIES_MESSAGE))));
            throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeedId,
                                                   UNSUPPORTED_GRANULARITIES_MESSAGE);
        }
        outbox.commit(new DKValidatedEvent(permissionId,
                                           granularity,
                                           wrapper.calculatedStart(),
                                           wrapper.calculatedEnd()));
        return new CreatedPermissionRequest(permissionId);
    }
}
