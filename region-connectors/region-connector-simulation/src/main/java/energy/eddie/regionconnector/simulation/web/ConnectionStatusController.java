package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.shared.cim.v0_82.pmd.IntermediatePermissionMarketDocument;
import energy.eddie.regionconnector.simulation.SimulationConnectorMetadata;
import energy.eddie.regionconnector.simulation.SimulationDataSourceInformation;
import energy.eddie.regionconnector.simulation.dtos.SetConnectionStatusRequest;
import energy.eddie.regionconnector.simulation.permission.request.SimulationPermissionRequest;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Period;
import java.time.ZoneOffset;

@RestController
public class ConnectionStatusController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusController.class);
    private final DocumentStreams streams;

    public ConnectionStatusController(DocumentStreams streams) {this.streams = streams;}

    @GetMapping(value = "/connection-status-values")
    public ResponseEntity<PermissionProcessStatus[]> connectionStatusValues() {
        return ResponseEntity.ok(PermissionProcessStatus.values());
    }

    @PostMapping(value = "/connection-status")
    public ResponseEntity<String> changeConnectionStatus(@RequestBody SetConnectionStatusRequest req) {
        LOGGER.info("Changing connection status of {} to {}", req.connectionId, req.connectionStatus);
        if (req.connectionId == null || req.connectionStatus == null || req.dataNeedId == null) {
            LOGGER.error(
                    "Mandatory attribute missing (connectionId,connectionStatus,dataNeedId) on ConnectionStatusMessage from frontend: {}",
                    req);
            return ResponseEntity.badRequest()
                                 .body("Mandatory attribute missing (connectionId,connectionStatus,dataNeedId) on ConnectionStatusMessage");
        }
        streams.publish(
                new ConnectionStatusMessage(
                        req.connectionId,
                        req.permissionId,
                        req.dataNeedId,
                        new SimulationDataSourceInformation(),
                        req.connectionStatus,
                        req.connectionStatus.toString()
                )
        );
        streams.publish(
                new IntermediatePermissionMarketDocument<PermissionRequest>(
                        new SimulationPermissionRequest(req),
                        SimulationConnectorMetadata.REGION_CONNECTOR_ID,
                        ignored -> null,
                        "N" + SimulationConnectorMetadata.getInstance().countryCode(),
                        ZoneOffset.UTC,
                        new ValidatedHistoricalDataDataNeed(new RelativeDuration(Period.ofYears(-3),
                                                                                 Period.ofYears(3),
                                                                                 null),
                                                            EnergyType.ELECTRICITY,
                                                            Granularity.PT5M,
                                                            Granularity.P1Y)
                ).toPermissionMarketDocument()
        );
        return ResponseEntity.ok(req.connectionId);
    }
}
