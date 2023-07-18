package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.server.Server;
import reactor.adapter.JdkFlowAdapter;
import reactor.util.annotation.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.Flow;

public class SimulationConnector implements RegionConnector {
    public static final String MDA_CODE = "sim";
    private static final String SRC_MAIN_PREFIX = "./region-connectors/region-connector-simulation/src/main/";

    @Nullable
    private Javalin javalin;

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, "Simulation", "de", basePath() + "/", 1);
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

    public static String basePath() {
        return "/region-connectors/" + MDA_CODE;
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        javalin = Javalin.create(config -> config.staticFiles.add(staticFileConfig -> {
            staticFileConfig.hostedPath = basePath();
            if (devMode) {
                staticFileConfig.directory = SRC_MAIN_PREFIX + "resources/public" + staticFileConfig.hostedPath;
                staticFileConfig.location = Location.EXTERNAL;
            } else {
                staticFileConfig.directory = "public" + staticFileConfig.hostedPath;
                staticFileConfig.location = Location.CLASSPATH;
            }
            staticFileConfig.mimeTypes.add("text/javascript", ".js");
            config.jetty.server(() -> new Server(address));
        }));
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
    public void close() {
        if (null != javalin) {
            javalin.close();
        }
    }
}
