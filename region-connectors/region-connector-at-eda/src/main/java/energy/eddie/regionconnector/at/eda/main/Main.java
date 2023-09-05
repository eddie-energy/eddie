package energy.eddie.regionconnector.at.eda.main;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;
import energy.eddie.regionconnector.at.eda.config.PropertiesAtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PropertiesPontonXPAdapterConfiguration;

import java.net.InetSocketAddress;
import java.util.Properties;

public class Main {

    /**
     * Starts the region connector and webapp, so it can be tested without running the framework
     */
    @SuppressWarnings("java:S106")
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        var in = EdaRegionConnector.class.getClassLoader().getResourceAsStream("regionconnector-at.properties");
        properties.load(in);

        PropertiesAtConfiguration atConfiguration = PropertiesAtConfiguration.fromProperties(properties);
        PontonXPAdapter edaAdapter = new PontonXPAdapter(new PropertiesPontonXPAdapterConfiguration(properties));

        try (RegionConnector regionConnectorAT = new EdaRegionConnector(atConfiguration, edaAdapter, new InMemoryPermissionRequestRepository())) {
            var hostname = properties.getProperty("hostname", "localhost");
            var port = Integer.parseInt(properties.getProperty("port", "8080"));

            regionConnectorAT.startWebapp(new InetSocketAddress(hostname, port), true);
            System.out.println("Started webapp on " + hostname + ":" + port);

            // wait for the user to press enter
            System.in.read();
        }
        System.exit(0);
    }

}
