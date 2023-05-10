package eddie.energy.regionconnector.at.ponton;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PontonXPAdapterConfig {
    private static final String ADAPTER_ID_KEY = "messenger.adapterId";
    private static final String ADAPTER_VERSION_KEY = "messenger.adapterVersion";
    private static final String HOSTNAME_KEY = "messenger.hostname";
    private static final String PORT_KEY = "messenger.port";
    private static final String WORK_FOLDER_KEY = "messenger.workFolder";
    private final String adapterId;
    private final String adapterVersion;
    private final String hostname;
    private final int port;
    private final String workFolder;

    private PontonXPAdapterConfig(Builder builder) {
        this.adapterId = builder.adapterId;
        this.adapterVersion = builder.adapterVersion;
        this.hostname = builder.hostname;
        this.port = builder.port;
        this.workFolder = builder.workFolder;
    }

    public static PontonXPAdapterConfig fromProperties(Properties properties) {
        return new Builder()
                .withAdapterId(properties.getProperty(ADAPTER_ID_KEY))
                .withAdapterVersion(properties.getProperty(ADAPTER_VERSION_KEY))
                .withHostname(properties.getProperty(HOSTNAME_KEY))
                .withPort(Integer.parseInt(properties.getProperty(PORT_KEY)))
                .withWorkFolder(properties.getProperty(WORK_FOLDER_KEY))
                .build();
    }

    // Getters for the properties
    public String getAdapterId() {
        return adapterId;
    }

    public String getAdapterVersion() {
        return adapterVersion;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getWorkFolder() {
        return workFolder;
    }

    public static class Builder {
        private final List<String> errorMessages = new ArrayList<>();
        private String adapterId;
        private String adapterVersion;
        private String hostname;
        private int port;
        private String workFolder;

        public Builder() {
        }

        public Builder withAdapterId(String adapterId) {
            this.adapterId = adapterId;
            return this;
        }

        public Builder withAdapterVersion(String adapterVersion) {
            this.adapterVersion = adapterVersion;
            return this;
        }

        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder withPort(int port) {
            if (port <= 0) {
                errorMessages.add("Invalid property: messenger.port");
            }
            this.port = port;
            return this;
        }

        public Builder withWorkFolder(String workFolder) {
            this.workFolder = workFolder;
            return this;
        }

        public PontonXPAdapterConfig build() {
            if (adapterId == null) {
                errorMessages.add("Missing property: messenger.adapterId");
            }
            if (adapterVersion == null) {
                errorMessages.add("Missing property: messenger.adapterVersion");
            }
            if (hostname == null) {
                errorMessages.add("Missing property: messenger.hostname");
            }
            if (workFolder == null) {
                errorMessages.add("Missing property: messenger.workFolder");
            }
            if (!errorMessages.isEmpty()) {
                throw new IllegalArgumentException(String.join("; ", errorMessages));
            }
            return new PontonXPAdapterConfig(this);
        }
    }
}
