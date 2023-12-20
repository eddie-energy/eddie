package energy.eddie.regionconnector.at.eda.ponton;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * This class contains all information needed for a PontonXPAdapter to establish a connection to a Ponton XP Messenger.
 */
public final class PropertiesPontonXPAdapterConfiguration implements PontonXPAdapterConfiguration {
    private static final String PROPERTY_IS_REQUIRED = "Property %s is required";
    private final Properties properties;

    public PropertiesPontonXPAdapterConfiguration(Properties properties) {
        this.properties = requireNonNull(properties);
    }

    public static PropertiesPontonXPAdapterConfiguration fromProperties(Properties properties) {
        requireNonNull(properties.getProperty(ADAPTER_ID_KEY), PROPERTY_IS_REQUIRED.formatted(ADAPTER_ID_KEY));
        requireNonNull(properties.getProperty(ADAPTER_VERSION_KEY), PROPERTY_IS_REQUIRED.formatted(ADAPTER_VERSION_KEY));
        requireNonNull(properties.getProperty(HOSTNAME_KEY), PROPERTY_IS_REQUIRED.formatted(HOSTNAME_KEY));
        requireNonNull(properties.getProperty(WORK_FOLDER_KEY), PROPERTY_IS_REQUIRED.formatted(WORK_FOLDER_KEY));
        var portString = properties.getProperty(PORT_KEY);
        var port = portString == null ? DEFAULT_PORT : Integer.parseInt(portString);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }

        return new PropertiesPontonXPAdapterConfiguration(properties);
    }

    private String get(String key) {
        return requireNonNull(properties.getProperty(key), PROPERTY_IS_REQUIRED.formatted(key));
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
        var port = properties.getProperty(PORT_KEY);
        return port == null ? DEFAULT_PORT : Integer.parseInt(port);
    }

    @Override
    public String workFolder() {
        return get(WORK_FOLDER_KEY);
    }

}
