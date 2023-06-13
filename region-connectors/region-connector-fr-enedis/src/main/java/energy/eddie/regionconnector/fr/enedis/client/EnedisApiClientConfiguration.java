package energy.eddie.regionconnector.fr.enedis.client;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.Nullable;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

public record EnedisApiClientConfiguration(String clientId, String clientSecret, String basePath) {
    private static final String CLIENT_ID_KEY_PROP = "enedis.clientId";
    private static final String CLIENT_SECRET_KEY_PROP = "enedis.clientSecret";
    private static final String BASE_PATH_KEY_PROP = "enedis.basePath";
    private static final String CLIENT_ID_KEY_ENV = "CLIENT_ID";
    private static final String CLIENT_SECRET_KEY_ENV = "CLIENT_SECRET";
    private static final String BASE_PATH_KEY_ENV = "BASE_PATH";

    public static EnedisApiClientConfiguration fromProperties(Properties properties) {
        var clientId = properties.getProperty(CLIENT_ID_KEY_PROP);
        requireNonNull(clientId, "Missing property: " + CLIENT_ID_KEY_PROP);
        var clientSecret = properties.getProperty(CLIENT_SECRET_KEY_PROP);
        requireNonNull(clientSecret, "Missing property: " + CLIENT_SECRET_KEY_PROP);
        var basePath = properties.getProperty(BASE_PATH_KEY_PROP);
        requireNonNull(basePath, "Missing property: " + BASE_PATH_KEY_PROP);

        return new EnedisApiClientConfiguration(clientId, clientSecret, basePath);
    }

    public static EnedisApiClientConfiguration fromEnvironment() {
        final Dotenv dotenv = Dotenv.configure()
                .filename("region-connectors/region-connector-fr-enedis/.env")
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        var clientId = dotenv.get(CLIENT_ID_KEY_ENV);
        requireNonNull(clientId, "Missing variable: " + CLIENT_ID_KEY_ENV);
        var clientSecret = dotenv.get(CLIENT_SECRET_KEY_ENV);
        requireNonNull(clientSecret, "Missing variable: " + CLIENT_SECRET_KEY_ENV);
        var basePath = dotenv.get(BASE_PATH_KEY_ENV);
        requireNonNull(basePath, "Missing variable: " + BASE_PATH_KEY_ENV);

        return new EnedisApiClientConfiguration(clientId, clientSecret, basePath);
    }

    public static class Builder {
        @Nullable
        private String clientId;
        @Nullable
        private String clientSecret;
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

        public Builder withBasePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public EnedisApiClientConfiguration build() {
            requireNonNull(clientId);
            requireNonNull(clientSecret);
            requireNonNull(basePath);

            return new EnedisApiClientConfiguration(clientId, clientSecret, basePath);
        }
    }
}