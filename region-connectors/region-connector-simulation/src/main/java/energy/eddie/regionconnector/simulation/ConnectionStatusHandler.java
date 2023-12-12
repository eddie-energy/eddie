package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.Nullable;

public class ConnectionStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusHandler.class);
    @Nullable
    private static ConnectionStatusHandler singleton;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusStreamSink = Sinks.many().multicast().onBackpressureBuffer();

    private ConnectionStatusHandler() {
    }

    public static synchronized ConnectionStatusHandler instance() {
        if (null == singleton) {
            ConnectionStatusHandler.singleton = new ConnectionStatusHandler();
        }
        return ConnectionStatusHandler.singleton;
    }

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusStreamSink.asFlux();
    }

    void initWebapp(Javalin app) {
        LOGGER.info("Initializing Javalin app");
        String basePath = SimulationConnectorMetadata.getInstance().id();
        app.get(basePath + "/api/connection-status-values", ctx -> ctx.json(PermissionProcessStatus.values()));
        app.post(basePath + "/api/connection-status", ctx -> {
            var req = ctx.bodyAsClass(SetConnectionStatusRequest.class);
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
            } else {
                LOGGER.error("Mandatory attribute missing (connectionId,connectionStatus,dataNeedId) on ConnectionStatusMessage from frontend: {}", ctx.body());
                ctx.status(HttpStatus.BAD_REQUEST);
            }
        });
    }

    public static class SetConnectionStatusRequest {
        @Nullable
        public String connectionId;
        @Nullable
        public String dataNeedId;
        @Nullable
        public PermissionProcessStatus connectionStatus;
    }

    private static class SimulationDataSourceInformation implements DataSourceInformation {
        @Override
        public String countryCode() {
            return "sim";
        }

        @Override
        public String regionConnectorId() {
            return "sim";
        }

        @Override
        public String permissionAdministratorId() {
            return "sim";
        }

        @Override
        public String meteredDataAdministratorId() {
            return "sim";
        }
    }
}