package energy.eddie.api.v0_82;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Flow.Publisher;

/**
 * A region connector connects MDAs to EDDIE. It implements conversion of the MDA specific data records
 * to the EDDIE CIM format {@link ConsumptionRecord} as well as the MDA and CA specifics related to these MDAs.
 * If offers:
 * <ul>
 *     <li>metadata describing the region connector and the supported MDAs</li>
 *     <li>data streams with all the machine processable data this region connector provides</li>
 *     <li>a web-UI component for the consent process</li>
 *     <li>methods to perform administrative tasks like closing a connection</li>
 * </ul>
 */
public interface RegionConnector extends AutoCloseable {
    /**
     * Get metadata describing this region connector.
     *
     * @return metadata object
     */
    RegionConnectorMetadata getMetadata();

    /**
     * Data stream of all connection status updates created by this region connector.
     *
     * @return connection status message stream that can be consumed only once
     */
    Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream();

    /**
     * Data stream of all EddieValidatedHistoricalDataMarketDocument created by this region connector.
     *
     * @return EddieValidatedHistoricalDataMarketDocument stream that can be consumed only once
     */
    Publisher<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream();

    /**
     * Terminates the permission at the permission administrator's system and closes the associated connection.
     *
     * @param permissionId unique id of the permission
     */
    void terminatePermission(String permissionId);

    /**
     * Starts the web-application handling the PA specifics of the consent workflow.
     *
     * @param address address the web server should be reachable. A specific port can be specified or any available port
     *                can be used. (pass 0 as a port then) If a specific port is given, it must be used.
     *                It also specifies the network interface which may be all
     *                interfaces or e.g. just the local loopback "localhost".
     * @param devMode if set to true, the region connector may put it's web server in development mode. e.g. serving
     *                static assets directly from the file system, disable caching and so on.
     * @return port the web-application is listening to (only relevant if port 0 is given in the address
     * @throws RuntimeException if the webserver cannot be started, e.g. if a specific port was given and this cannot
     *                          be used
     */
    int startWebapp(InetSocketAddress address, boolean devMode);

    /**
     * Returns the health of the region connectors and its services.
     *
     * @return a map of the health of the used services by the region connector.
     */
    Map<String, HealthState> health();
}
