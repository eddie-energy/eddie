package energy.eddie.aiida.utils;

import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;

public class MqttConfig {
    public static final Integer DEFAULT_KEEP_ALIVE_INTERVAL = 60;
    private String serverURI;
    private String subscribeTopic;
    private Boolean cleanStart;
    private Boolean automaticReconnect;
    private Integer keepAliveInterval;
    @Nullable
    private String username;
    @Nullable
    private String password;

    // Must only be constructed by builder
    @SuppressWarnings({"NullAway.Init", "unused"})
    private MqttConfig() {
    }

    public MqttConfig(MqttConfigBuilder builder) {
        this.serverURI = builder.serverURI;
        this.subscribeTopic = builder.subscribeTopic;
        this.cleanStart = builder.cleanStart;
        this.automaticReconnect = builder.automaticReconnect;
        this.keepAliveInterval = builder.keepAliveInterval;
        this.username = builder.username;
        this.password = builder.password;
    }

    public String serverURI() {
        return serverURI;
    }

    public String subscribeTopic() {
        return subscribeTopic;
    }

    public Boolean cleanStart() {
        return cleanStart;
    }

    public Boolean automaticReconnect() {
        return automaticReconnect;
    }

    public Integer keepAliveInterval() {
        return keepAliveInterval;
    }

    @Nullable
    public String username() {
        return username;
    }

    @Nullable
    public String password() {
        return password;
    }

    public static class MqttConfigBuilder {
        private final String serverURI;
        private final String subscribeTopic;
        private Boolean cleanStart = false;
        private Boolean automaticReconnect = true;
        private Integer keepAliveInterval = MqttConfig.DEFAULT_KEEP_ALIVE_INTERVAL;
        @Nullable
        private String username = null;
        @Nullable
        private String password = null;

        /**
         * Creates a new builder for a {@link MqttConfig} with the required properties.
         *
         * @param serverURI      URI of the server to connect to.
         * @param subscribeTopic Topic to which should be subscribed.
         */
        public MqttConfigBuilder(String serverURI, String subscribeTopic) {
            this.serverURI = serverURI;
            this.subscribeTopic = subscribeTopic;
        }

        /**
         * @param cleanStart If true, server and client will not retain state across client restarts.
         */
        public MqttConfigBuilder setCleanStart(Boolean cleanStart) {
            this.cleanStart = cleanStart;
            return this;
        }

        /**
         * @param automaticReconnect If true, the MQTT client will automatically try to reconnect to the server if connection is lost.
         */
        public MqttConfigBuilder setAutomaticReconnect(Boolean automaticReconnect) {
            this.automaticReconnect = automaticReconnect;
            return this;
        }

        /**
         * Sets the keepAliveInterval.
         *
         * @param keepAliveInterval Duration in seconds for the keepAliveInterval, see {@link MqttConnectionOptions#setKeepAliveInterval(int)}
         * @throws IllegalArgumentException If the supplied keepAliveInterval is negative.
         */
        public MqttConfigBuilder setKeepAliveInterval(Integer keepAliveInterval) throws IllegalArgumentException {
            if (keepAliveInterval < 0)
                throw new IllegalArgumentException("keepAliveInterval needs to be <= 0 seconds");

            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        /**
         * @param username Username to use for authentication to the server. When using a username, a password has to be specified as well.
         */
        public MqttConfigBuilder setUsername(String username) {
            this.username = username;
            return this;
        }

        /**
         * MQTT v5 specification permits specifying a password without a username.
         *
         * @param password Password to use for authentication to the server.
         * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901043">MQTT v5 specification</a>
         */
        public MqttConfigBuilder setPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Builds a new {@link MqttConfig} with the supplied values and uses default values if none have been specified.
         *
         * @return MqttConfig with the specified values.
         * @throws IllegalArgumentException If a username has been specified, but no password.
         */
        public MqttConfig build() throws IllegalArgumentException {
            if (username != null && password == null)
                throw new IllegalArgumentException("When supplying a username, a password has to be supplied as well");

            return new MqttConfig(this);
        }
    }
}
