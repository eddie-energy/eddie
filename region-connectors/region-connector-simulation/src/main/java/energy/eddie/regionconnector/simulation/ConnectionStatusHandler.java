package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConnectionStatusMessage;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ConnectionStatusHandler {

    @Nullable
    private static ConnectionStatusHandler singleton;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusHandler.class);

    private ConnectionStatusHandler() {
    }

    private Sinks.Many<ConnectionStatusMessage> connectionStatusStreamSink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusStreamSink.asFlux();
    }

    public static class SetConnectionStatusRequest {
        @Nullable
        public String connectionId;
        @Nullable
        public ConnectionStatusMessage.Status connectionStatus;
    }

    void initWebapp(Javalin app) {
        LOGGER.info("Initializing Javalin app");
        app.get(SimulationConnector.basePath() + "/api/connection-status-values", ctx -> ctx.json(ConnectionStatusMessage.Status.values()));
        app.post(SimulationConnector.basePath() + "/api/connection-status", ctx -> {
            var req = ctx.bodyAsClass(SetConnectionStatusRequest.class);
            LOGGER.info("Changing connection status of {} to {}", req.connectionId, req.connectionStatus);
            var now = ZonedDateTime.now(ZoneId.systemDefault());
            if (null == connectionStatusStreamSink) {
                throw new IllegalStateException("connectionStatusStreamSink not initialized yet");
            }
            if (null != req.connectionId && null != req.connectionStatus) {
                connectionStatusStreamSink.tryEmitNext(new ConnectionStatusMessage(req.connectionId, req.connectionId, now, req.connectionStatus, req.connectionStatus.toString()));
            } else {
                ctx.status(HttpStatus.BAD_REQUEST);
            }
        });
    }

    public static synchronized ConnectionStatusHandler instance() {
        if (null == singleton) {
            ConnectionStatusHandler.singleton = new ConnectionStatusHandler();
        }
        return ConnectionStatusHandler.singleton;
    }
}
