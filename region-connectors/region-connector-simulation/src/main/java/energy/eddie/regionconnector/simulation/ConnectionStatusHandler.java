package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsentStatus;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.ZonedDateTime;

public enum ConnectionStatusHandler {
    INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusHandler.class);

    ConnectionStatusHandler() {
        connectionStatusStream = Flux.create(connectionStatusStreamSink -> this.connectionStatusStreamSink = connectionStatusStreamSink);
    }

    private final Flux<ConnectionStatusMessage> connectionStatusStream;
    private FluxSink<ConnectionStatusMessage> connectionStatusStreamSink;

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusStream;
    }

    public static class SetConnectionStatusRequest {
        public String connectionId;
        public ConsentStatus consentStatus;
    }

    void initWebapp(Javalin app) {
        LOGGER.info("initializing Javalin app");
        app.get(SimulationConnector.basePath() + "/api/consent-status-values", ctx -> ctx.json(ConsentStatus.values()));
        app.post(SimulationConnector.basePath() + "/api/consent-status", ctx -> {
            var req = ctx.bodyAsClass(SetConnectionStatusRequest.class);
            LOGGER.info("changing connection status of {} to {}", req.connectionId, req.consentStatus);
            connectionStatusStreamSink.next(new ConnectionStatusMessage(req.connectionId, req.connectionId, ZonedDateTime.now(), req.consentStatus));
        });
    }
}
