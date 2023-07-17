package energy.eddie.regionconnector.at.eda.main;

import energy.eddie.regionconnector.at.api.RegionConnectorAT;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;
import energy.eddie.regionconnector.at.eda.InMemoryEdaIdMapper;
import energy.eddie.regionconnector.at.eda.config.PropertiesAtConfiguration;
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

        try (RegionConnectorAT regionConnectorAT = new EdaRegionConnector(atConfiguration, edaAdapter, new InMemoryEdaIdMapper())) {
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
