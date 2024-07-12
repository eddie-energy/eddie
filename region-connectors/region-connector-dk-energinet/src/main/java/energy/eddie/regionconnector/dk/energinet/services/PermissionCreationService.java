package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.events.DKValidatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkCreatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkMalformedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static energy.eddie.regionconnector.dk.energinet.utils.JwtValidations.isValidUntil;

@Service
public class PermissionCreationService {
    private static final String UNSUPPORTED_GRANULARITIES_MESSAGE = "This region connector does not support these granularities";
    private static final String UNSUPPORTED_DATA_NEED_MESSAGE = "This region connector only supports validated historical data or accounting point data needs.";
    private static final String DATA_NEED_ID = "dataNeedId";
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;

    public PermissionCreationService(
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService
    ) {
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
        this.dataNeedCalculationService = dataNeedCalculationService;
    }

    /**
     * Creates a new {@link PermissionRequest}, validates it and sends it to the permission administrator.
     *
     * @param requestForCreation Dto that contains the necessary information for this permission request.
     * @return The created PermissionRequest
     */
    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation requestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException, InvalidRefreshTokenException {
        var permissionId = UUID.randomUUID().toString();
        var dataNeedId = requestForCreation.dataNeedId();
        outbox.commit(new DkCreatedEvent(permissionId,
                                         requestForCreation.connectionId(),
                                         dataNeedId,
                                         requestForCreation.meteringPoint(),
                                         requestForCreation.refreshToken()));
        var dataNeed = dataNeedsService.findById(dataNeedId);
        if (dataNeed.isEmpty()) {
            outbox.commit(new DkMalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, "Unknown DataNeed")));
            throw new DataNeedNotFoundException(dataNeedId);
        }
        var calculation = dataNeedCalculationService.calculate(dataNeed.get());
        var timeframe = calculation.energyDataTimeframe();
        if (!calculation.supportsDataNeed()) {
            outbox.commit(new DkMalformedEvent(permissionId,
                                               new AttributeError(DATA_NEED_ID, UNSUPPORTED_DATA_NEED_MESSAGE)));
            throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeedId, UNSUPPORTED_DATA_NEED_MESSAGE);
        }
        if (calculation.permissionTimeframe() != null
            && !isValidUntil(requestForCreation.refreshToken(), calculation.permissionTimeframe().end())) {
            outbox.commit(
                    new DkMalformedEvent(
                            permissionId,
                            new AttributeError("refreshToken",
                                               "Refresh Token is either malformed or is not valid until the end of the requested permission")
                    )
            );
            throw new InvalidRefreshTokenException();
        }
        if (dataNeed.get() instanceof ValidatedHistoricalDataDataNeed) {
            createValidatedHistoricalDataEvent(calculation, permissionId, dataNeedId, timeframe);
        } else {
            createAccountingPointMasterDataEvent(permissionId);
        }
        return new CreatedPermissionRequest(permissionId);
    }

    private void createValidatedHistoricalDataEvent(
            DataNeedCalculation calculation,
            String permissionId,
            String dataNeedId,
            Timeframe timeframe
    ) throws UnsupportedDataNeedException {
        if (calculation.granularities() == null || calculation.granularities().isEmpty()) {
            outbox.commit(new DkMalformedEvent(permissionId,
                                               new AttributeError(DATA_NEED_ID, UNSUPPORTED_GRANULARITIES_MESSAGE)));
            throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeedId, UNSUPPORTED_GRANULARITIES_MESSAGE);
        }

        var minimalViableGranularity = calculation.granularities().getFirst();
        Granularity requestGranularity = switch (minimalViableGranularity) {
            case P1D, P1M, P1Y -> minimalViableGranularity; // these are always available
            default -> null; // the rest needs to be checked when the permission is accepted
        };

        outbox.commit(new DKValidatedEvent(
                permissionId,
                requestGranularity,
                timeframe.start(),
                timeframe.end()
        ));
    }

    private void createAccountingPointMasterDataEvent(String permissionId) {
        LocalDate today = LocalDate.now(DK_ZONE_ID);
        outbox.commit(new DKValidatedEvent(
                permissionId,
                null,
                today,
                today
        ));
    }
}
