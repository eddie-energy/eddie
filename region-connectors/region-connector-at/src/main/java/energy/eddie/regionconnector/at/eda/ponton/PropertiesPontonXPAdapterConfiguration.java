package energy.eddie.regionconnector.at.eda.ponton;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * This class contains all information needed for a PontonXPAdapter to establish a connection to a Ponton XP Messenger.
 */
public final class PropertiesPontonXPAdapterConfiguration implements PontonXPAdapterConfiguration {
    private static final String PREFIX = "region-connector.at.eda.ponton.messenger.";

    private static final String ADAPTER_ID_KEY = PREFIX + "adapterId";
    private static final String ADAPTER_VERSION_KEY = PREFIX + "adapterVersion";
    private static final String HOSTNAME_KEY = PREFIX + "hostname";
    private static final String PORT_KEY = PREFIX + "port";
    private static final String WORK_FOLDER_KEY = PREFIX + "workFolder";
    private static final String DEFAULT_ADAPTER_PORT = "2600";
    private final Properties properties;

    public PropertiesPontonXPAdapterConfiguration(Properties properties) {
        this.properties = requireNonNull(properties);
    }

    public static PropertiesPontonXPAdapterConfiguration fromProperties(Properties properties) {
        requireNonNull(properties.getProperty(ADAPTER_ID_KEY), "Property %s is required".formatted(ADAPTER_ID_KEY));
        requireNonNull(properties.getProperty(ADAPTER_VERSION_KEY), "Property %s is required".formatted(ADAPTER_VERSION_KEY));
        requireNonNull(properties.getProperty(HOSTNAME_KEY), "Property %s is required".formatted(HOSTNAME_KEY));
        requireNonNull(properties.getProperty(WORK_FOLDER_KEY), "Property %s is required".formatted(WORK_FOLDER_KEY));
        var port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_ADAPTER_PORT));
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }

        return new PropertiesPontonXPAdapterConfiguration(properties);
    }

    private String get(String key) {
        return requireNonNull(properties.getProperty(key), "Property %s is required".formatted(key));
    }

    @Override
    public String adapterId() {
        return get(ADAPTER_ID_KEY);
    }

    @Override
    public String adapterVersion() {
        return get(ADAPTER_VERSION_KEY);
    }

    @Override
    public String hostname() {
        return get(HOSTNAME_KEY);
    }

    @Override
    public int port() {
        return Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_ADAPTER_PORT));
    }

    @Override
    public String workFolder() {
        return get(WORK_FOLDER_KEY);
    }

}
