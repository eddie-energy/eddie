package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.*;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import org.eclipse.jetty.server.Server;
import reactor.adapter.JdkFlowAdapter;
import reactor.util.annotation.Nullable;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;

public class SimulationConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider,
        Mvp1ConsumptionRecordProvider {

    @Nullable
    private Javalin javalin;

    @Override
    public RegionConnectorMetadata getMetadata() {
        return SimulationConnectorMetadata.getInstance();
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(ConnectionStatusHandler.instance().getConnectionStatusMessageStream());
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(ConsumptionRecordHandler.instance().getConsumptionRecordStream());
    }

    @Override
    public void terminatePermission(String permissionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        javalin = Javalin.create(config -> config.jetty.server(() -> new Server(address)));
        String basePath = SimulationConnectorMetadata.getInstance().id();
        String ceJsPath = basePath + "/ce.js";

        javalin.get(ceJsPath, context -> {
            context.contentType(ContentType.TEXT_JS);
            context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public/region-connectors/simulation/ce.js")));
        });

        javalin.get(basePath + "/produce-consumption-records.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public/produce-consumption-records.js")));
        });

        javalin.get(basePath + "/set-connection-status.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public/set-connection-status.js")));
        });

        javalin.get(basePath + "/simulation.html", context -> {
            context.contentType(ContentType.TEXT_HTML);
            context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public/simulation.html")));
        });


        ConnectionStatusHandler.instance().initWebapp(javalin);
        ConsumptionRecordHandler.instance().initWebapp(javalin);
        javalin.start();
        final var jetty = javalin.jettyServer();
        if (null == jetty) { // that's accepted by all code checkers.. :-D
            throw new NullPointerException("Cannot start Javalin");
        }
        return jetty.getServerPort();
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of();
    }

    @Override
    public void close() {
        if (null != javalin) {
            javalin.close();
        }
    }
}