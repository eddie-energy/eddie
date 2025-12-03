package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.de.eta.DeEtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.de.eta.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.de.eta.permission.events.CreatedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.MalformedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.RetransmitRequestedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriTemplate;

import java.util.UUID;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;

@RestController
public class PermissionRequestController {
    public static final String DATA_NEED_FIELD = "dataNeedId";
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;

    public PermissionRequestController(Outbox outbox,
                                       DataNeedCalculationService<DataNeed> dataNeedCalculationService) {
        this.outbox = outbox;
        this.dataNeedCalculationService = dataNeedCalculationService;
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(
            @RequestBody @Valid PermissionRequestForCreation dto
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();

        // Commit CreatedEvent to Outbox
        var created = new CreatedEvent(permissionId, dto.dataNeedId(), dto.connectionId());
        outbox.commit(created);

        // Calculate data need and branch into events
        var calc = dataNeedCalculationService.calculate(dto.dataNeedId());
        switch (calc) {
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new MalformedEvent(permissionId,
                        new AttributeError(DATA_NEED_FIELD, "Data need not found")));
                throw new DataNeedNotFoundException(dto.dataNeedId());
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new MalformedEvent(permissionId,
                        new AttributeError(DATA_NEED_FIELD, message)));
                throw new UnsupportedDataNeedException(DeEtaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                        dto.dataNeedId(), message);
            }
            case ValidatedHistoricalDataDataNeedResult(var granularities, var permissionTimeframe, Timeframe energyTimeframe) -> {
                var gran = granularities.isEmpty() ? null : granularities.getFirst();
                outbox.commit(new ValidatedEvent(permissionId,
                        dto.connectionId(),
                        dto.dataNeedId(),
                        gran,
                        energyTimeframe.start(),
                        energyTimeframe.end()));
            }
            default -> {
                // No action required for other calculation results.
            }
        }

        var location = new UriTemplate(CONNECTION_STATUS_STREAM).expand(permissionId);
        return ResponseEntity.created(location).body(new CreatedPermissionRequest(permissionId));
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST + "/{permissionId}/retransmit")
    public ResponseEntity<Void> retransmit(@PathVariable("permissionId") String permissionId) {
        outbox.commit(new RetransmitRequestedEvent(permissionId));
        return ResponseEntity.accepted().build();
    }
}
