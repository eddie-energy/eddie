package energy.eddie.regionconnector.at.eda.ponton;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;

/**
 * This interface defines all information needed for a PontonXPAdapter to establish a connection to a Ponton XP
 * Messenger.
 */
public interface PontonXPAdapterConfiguration {
    int DEFAULT_PORT = 2600;
    String PREFIX = AtConfiguration.PREFIX + "ponton.messenger.";
    String ADAPTER_ID_KEY = PREFIX + "adapter.id";
    String ADAPTER_VERSION_KEY = PREFIX + "adapter.version";
    String HOSTNAME_KEY = PREFIX + "hostname";
    String PORT_KEY = PREFIX + "port";
    String API_ENDPOINT_KEY = PREFIX + "api.endpoint";
    String WORK_FOLDER_KEY = PREFIX + "folder";

    /**
     * ID of the adapter that will be used by the Ponton XP Messenger. The value used for this should be configured as
     * the default adapter in the messenger
     */
    String adapterId();

    /**
     * Version of the adapter
     */
    String adapterVersion();

    /**
     * Address of the Ponton XP Messenger
     */
    String hostname();

    /**
     * API endpoint of the Ponton XP Messenger
     */
    String apiEndpoint();

    /**
     * Port of the Ponton XP Messenger adapter interface (default: 2600)
     */
    int port();

    /**
     * Path to the folder where the Ponton XP Adapter will store its files like id.dat which is used by the messenger to
     * identify the adapter.
     */
    String workFolder();
}
