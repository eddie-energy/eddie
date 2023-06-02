package energy.eddie.regionconnector.fr.enedis.client;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EnedisApiClientConfiguration {
    private static final String CLIENT_ID_KEY_PROP = "enedis.clientId";
    private static final String CLIENT_SECRET_KEY_PROP = "enedis.clientSecret";
    private static final String HOSTNAME_KEY_PROP = "enedis.hostname";
    private static final String BASE_PATH_KEY_PROP = "enedis.basePath";
    private static final String CLIENT_ID_KEY_ENV = "CLIENT_ID";
    private static final String CLIENT_SECRET_KEY_ENV = "CLIENT_SECRET";
    private static final String HOSTNAME_KEY_ENV = "HOSTNAME";
    private static final String BASE_PATH_KEY_ENV = "BASE_PATH";
    @Nullable
    private final String clientId;
    @Nullable
    private final String clientSecret;
    @Nullable
    private final String hostname;
    @Nullable
    private final String basePath;

    private EnedisApiClientConfiguration(Builder builder) {
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.hostname = builder.hostname;
        this.basePath = builder.basePath;
    }

    public static EnedisApiClientConfiguration fromProperties(Properties properties) {
        return new Builder()
                .withClientId(properties.getProperty(CLIENT_ID_KEY_PROP))
                .withClientSecret(properties.getProperty(CLIENT_SECRET_KEY_PROP))
                .withHostname(properties.getProperty(HOSTNAME_KEY_PROP))
                .withBasePath(properties.getProperty(BASE_PATH_KEY_PROP))
                .build();
    }

    public static EnedisApiClientConfiguration fromEnvironment() {
        final Dotenv dotenv = Dotenv.configure()
                .filename("region-connectors/region-connector-fr-enedis/.env")
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        return new Builder()
                .withClientId(dotenv.get(CLIENT_ID_KEY_ENV))
                .withClientSecret(dotenv.get(CLIENT_SECRET_KEY_ENV))
                .withHostname(dotenv.get(HOSTNAME_KEY_ENV))
                .withBasePath(dotenv.get(BASE_PATH_KEY_ENV))
                .build();
    }

    // Getters for the properties
    @Nullable
    public String getClientId() {
        return clientId;
    }
    @Nullable
    public String getClientSecret() {
        return clientSecret;
    }
    @Nullable
    public String getHostname() {
        return hostname;
    }
    @Nullable
    public String getBasePath() {
        return basePath;
    }

    public static class Builder {
        private final List<String> errorMessages = new ArrayList<>();
        @Nullable
        private String clientId;
        @Nullable
        private String clientSecret;
        @Nullable
        private String hostname;
        @Nullable
        private String basePath;

        public Builder() {
        }

        public Builder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder withBasePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public EnedisApiClientConfiguration build() {
            if (clientId == null) {
                errorMessages.add("Missing property: clientId");
            }
            if (clientSecret == null) {
                errorMessages.add("Missing property: clientSecret");
            }
            if (hostname == null) {
                errorMessages.add("Missing property: hostname");
            }
            if (basePath == null) {
                errorMessages.add("Missing property: basePath");
            }
            if (!errorMessages.isEmpty()) {
                throw new IllegalStateException(String.join("; ", errorMessages));
            }
            return new EnedisApiClientConfiguration(this);
        }
    }
}