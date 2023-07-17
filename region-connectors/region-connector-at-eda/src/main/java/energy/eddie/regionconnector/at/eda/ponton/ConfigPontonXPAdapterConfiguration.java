package energy.eddie.regionconnector.at.eda.ponton;

import org.eclipse.microprofile.config.Config;

import static java.util.Objects.requireNonNull;

public class ConfigPontonXPAdapterConfiguration implements PontonXPAdapterConfiguration {
    private final Config config;

    public ConfigPontonXPAdapterConfiguration(Config config) {
        requireNonNull(config);
        this.config = config;
    }

    @Override
    public String adapterId() {
        return config.getValue(ADAPTER_ID_KEY, String.class);
    }

    @Override
    public String adapterVersion() {
        return config.getValue(ADAPTER_VERSION_KEY, String.class);
    }

    @Override
    public String hostname() {
        return config.getValue(HOSTNAME_KEY, String.class);
    }

    @Override
    public int port() {
        return config.getOptionalValue(PORT_KEY, Integer.class).orElse(DEFAULT_PORT);
    }

    @Override
    public String workFolder() {
        return config.getValue(WORK_FOLDER_KEY, String.class);
    }
}
