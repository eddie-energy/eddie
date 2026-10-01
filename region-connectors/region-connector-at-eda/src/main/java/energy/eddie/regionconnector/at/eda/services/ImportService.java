// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import energy.eddie.regionconnector.at.eda.permission.request.events.ImportEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class ImportService {
    private final DataNeedCalculationService calculationService;
    private final Outbox outbox;

    public ImportService(DataNeedCalculationService calculationService, Outbox outbox) {
        this.calculationService = calculationService;
        this.outbox = outbox;
    }

    public CreatedPermissionRequest importPermissionRequest(PermissionRequestToImport request) throws UnsupportedDataNeedException, DataNeedNotFoundException {
        var dataNeed = calculationService.calculate(request.dataNeedId(), request.creationDateTime());
        var event = switch (dataNeed) {
            case ValidatedHistoricalDataDataNeedResult res -> createImportEvent(request,
                                                                                res.energyTimeframe().start(),
                                                                                res.energyTimeframe().end(),
                                                                                res.granularities());
            case CESUJoinRequestDataNeedResult res -> createImportEvent(request,
                                                                        res.permissionTimeframe().start(),
                                                                        null,
                                                                        res.supportedGranularities());
            case DataNeedNotFoundResult ignored -> throw new DataNeedNotFoundException(request.dataNeedId());
            case DataNeedNotSupportedResult(var message) ->
                    throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, request.dataNeedId(), message);
            default -> throw new UnsupportedDataNeedException(request.dataNeedId(),
                                                              "Only Data Needs for Validated Historical Data and CESU Join Requests are supported for imports");
        };
        outbox.commit(event);
        return new CreatedPermissionRequest(List.of(event.permissionId()));
    }

    private ImportEvent createImportEvent(
            PermissionRequestToImport request,
            LocalDate permissionStart,
            @Nullable LocalDate permissionEnd,
            List<Granularity> granularities
    ) {
        return new ImportEvent(
                UUID.randomUUID().toString(),
                request.connectionId(),
                request.meteringPointId(),
                request.dataNeedId(),
                request.dsoId(),
                request.consentId(),
                permissionStart,
                permissionEnd,
                AllowedGranularity.valueOf(granularities.getFirst()),
                request.creationDateTime(),
                request.meterReadingStart(),
                request.meterReadingEnd()
        );
    }
}
