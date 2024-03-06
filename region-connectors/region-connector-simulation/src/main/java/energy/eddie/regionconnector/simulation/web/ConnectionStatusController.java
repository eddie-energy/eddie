package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.SimulationDataSourceInformation;
import energy.eddie.regionconnector.simulation.dtos.SetConnectionStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
public class ConnectionStatusController implements Mvp1ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusController.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusStreamSink = Sinks.many().multicast().onBackpressureBuffer();

    @GetMapping(value = "/api/connection-status-values")
    public ResponseEntity<PermissionProcessStatus[]> connectionStatusValues() {
        return ResponseEntity.ok(PermissionProcessStatus.values());
    }

    @PostMapping(value = "/api/connection-status")
    public ResponseEntity<String> changeConnectionStatus(@RequestBody SetConnectionStatusRequest req) {
        LOGGER.info("Changing connection status of {} to {}", req.connectionId, req.connectionStatus);
        if (req.connectionId != null && req.connectionStatus != null && req.dataNeedId != null) {
            connectionStatusStreamSink.tryEmitNext(
                    new ConnectionStatusMessage(
                            req.connectionId,
                            req.connectionId,
                            req.dataNeedId,
                            new SimulationDataSourceInformation(),
                            req.connectionStatus,
                            req.connectionStatus.toString()
                    )
            );
            return ResponseEntity.ok(req.connectionId);
        } else {
            LOGGER.error("Mandatory attribute missing (connectionId,connectionStatus,dataNeedId) on ConnectionStatusMessage from frontend: {}", req);
            return ResponseEntity.badRequest().body("Mandatory attribute missing (connectionId,connectionStatus,dataNeedId) on ConnectionStatusMessage");
        }
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusStreamSink.asFlux();
    }

    @Override
    public void close() {
        connectionStatusStreamSink.tryEmitComplete();
    }
}
