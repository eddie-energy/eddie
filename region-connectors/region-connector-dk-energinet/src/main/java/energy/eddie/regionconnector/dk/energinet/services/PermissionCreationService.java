// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
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
import static energy.eddie.regionconnector.shared.jwt.JwtValidations.isValidUntil;

@Service
public class PermissionCreationService {
    private static final String DATA_NEED_ID = "dataNeedId";
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;

    public PermissionCreationService(
            Outbox outbox,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService
    ) {
        this.outbox = outbox;
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
        var calculation = dataNeedCalculationService.calculate(dataNeedId);
        switch (calculation) {
            case AiidaDataNeedResult ignored -> {
                String message = "AiidaDataDataNeedResult not supported!";
                outbox.commit(new DkMalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
            }
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new DkMalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, "Unknown DataNeed")));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new DkMalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
            }
            case AccountingPointDataNeedResult(Timeframe permissionTimeframe) -> {
                validateToken(requestForCreation, permissionTimeframe, permissionId);
                createAccountingPointMasterDataEvent(permissionId);
            }
            case ValidatedHistoricalDataDataNeedResult vhdDataNeedResult -> {
                validateToken(requestForCreation, vhdDataNeedResult.permissionTimeframe(), permissionId);
                createValidatedHistoricalDataEvent(vhdDataNeedResult,
                                                   permissionId,
                                                   vhdDataNeedResult.energyTimeframe());
            }
        }
        return new CreatedPermissionRequest(permissionId);
    }

    private void validateToken(
            PermissionRequestForCreation requestForCreation,
            Timeframe permissionTimeframe,
            String permissionId
    ) throws InvalidRefreshTokenException {
        if (!isValidUntil(requestForCreation.refreshToken(), permissionTimeframe.end())) {
            outbox.commit(
                    new DkMalformedEvent(
                            permissionId,
                            new AttributeError("refreshToken",
                                               "Refresh Token is either malformed or is not valid until the end of the requested permission")
                    )
            );
            throw new InvalidRefreshTokenException();
        }
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

    private void createValidatedHistoricalDataEvent(
            ValidatedHistoricalDataDataNeedResult calculation,
            String permissionId,
            Timeframe timeframe
    ) {
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
}
