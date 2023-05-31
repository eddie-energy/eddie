package energy.eddie.regionconnector.at.eda.ponton;

import jakarta.annotation.Nullable;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * This class contains all information needed for a PontonXPAdapter to establish a connection to a Ponton XP Messenger.
 * @param adapterId ID of the adapter that will be used by the Ponton XP Messenger. This value used for this should be configured as the default adapter in the messenger
 * @param adapterVersion Version of the adapter
 * @param hostname Address of the Ponton XP Messenger
 * @param port Port of the Ponton XP Messenger adapter interface (default: 2600)
 * @param workFolder Path to the folder where the Ponton XP Adapter will store its files like id.dat which is used by the messenger to identify the adapter.
 */
public record PontonXPAdapterConfig(String adapterId, String adapterVersion, String hostname, int port,
                                    String workFolder) {
    private static final String ADAPTER_ID_KEY = "messenger.adapterId";
    private static final String ADAPTER_VERSION_KEY = "messenger.adapterVersion";
    private static final String HOSTNAME_KEY = "messenger.hostname";
    private static final String PORT_KEY = "messenger.port";
    private static final String WORK_FOLDER_KEY = "messenger.workFolder";

    private static final String DEFAULT_ADAPTER_PORT = "2600";

    public static PontonXPAdapterConfig fromProperties(Properties properties) {
        var adapterId = properties.getProperty(ADAPTER_ID_KEY);
        requireNonNull(adapterId, "Missing property: " + ADAPTER_ID_KEY);
        var adapterVersion = properties.getProperty(ADAPTER_VERSION_KEY);
        requireNonNull(adapterVersion, "Missing property: " + ADAPTER_VERSION_KEY);
        var hostname = properties.getProperty(HOSTNAME_KEY);
        requireNonNull(hostname, "Missing property: " + HOSTNAME_KEY);
        var workFolder = properties.getProperty(WORK_FOLDER_KEY);
        requireNonNull(workFolder, "Missing property: " + WORK_FOLDER_KEY);
        var port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_ADAPTER_PORT));

        return new PontonXPAdapterConfig(adapterId, adapterVersion, hostname, port, workFolder);
    }

    public static class Builder {
        @Nullable
        private String adapterId;
        @Nullable

        private String adapterVersion;
        @Nullable

        private String hostname;
        private int port = Integer.parseInt(DEFAULT_ADAPTER_PORT);
        @Nullable
        private String workFolder;

        /**
         * Builder for {@link PontonXPAdapterConfig}
         */
        public Builder() {
        }

        /**
         * ID of the adapter that will be used by the Ponton XP Messenger. This value used for this should be configured as the default adapter in the messenger
         */
        public Builder withAdapterId(String adapterId) {
            this.adapterId = adapterId;
            return this;
        }

        /**
         * Version of the adapter
         */
        public Builder withAdapterVersion(String adapterVersion) {
            this.adapterVersion = adapterVersion;
            return this;
        }

        /**
         * Address of the Ponton XP Messenger
         */
        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }
        /**
         * Port of the Ponton XP Messenger adapter interface (default: 2600)
         */
        public Builder withPort(int port) {
            if (port <= 0) {
                throw new IllegalArgumentException("Port must be a positive integer");
            }

            this.port = port;
            return this;
        }

        /**
         * Path to the folder where the Ponton XP Adapter will store its files like id.dat which is used by the messenger to identify the adapter.
         */
        public Builder withWorkFolder(String workFolder) {
            this.workFolder = workFolder;
            return this;
        }

        public PontonXPAdapterConfig build() {
            requireNonNull(adapterId);
            requireNonNull(adapterVersion);
            requireNonNull(hostname);
            requireNonNull(workFolder);

            return new PontonXPAdapterConfig(adapterId, adapterVersion, hostname, port, workFolder);
        }
    }
}
