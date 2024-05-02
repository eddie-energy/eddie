package energy.eddie.regionconnector.aiida.config;

import jakarta.annotation.Nullable;
import org.springframework.web.util.UriTemplate;

public interface AiidaConfiguration {
    String PREFIX = "region-connector.aiida.";
    String CUSTOMER_ID = PREFIX + "customer.id";
    String BCRYPT_STRENGTH = PREFIX + "bcrypt.strength";
    String MQTT_SERVER_URI = PREFIX + "mqtt.server.uri";
    String MQTT_USERNAME = PREFIX + "mqtt.username";
    String MQTT_PASSWORD = PREFIX + "mqtt.password";
    String EDDIE_PUBLIC_URL = "eddie.public.url";

    /**
     * Customer Identifier
     *
     * @return customerId
     */
    String customerId();

    /**
     * Strength to be used by {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}.
     *
     * @see <a
     * href="https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCrypt.html">Spring
     * documentation for BCryptPasswordEncoder</a>
     */
    int bCryptStrength();

    /**
     * URL which should be used by AIIDA instances as an endpoint for the handshake. Is a template with a placeholder
     * {@code permissionId} that can be replaced by using {@link UriTemplate}. A PATCH request to this URL can be used
     * to change the state of the permission request (e.g. to ACCEPTED), whereas a GET request will return the details
     * of the permission request.
     */
    String handshakeUrl();

    /**
     * URI of the MQTT broker to which termination requests should be published.
     */
    String mqttServerUri();

    /**
     * Optional username to use to authenticate to the MQTT broker.
     */
    @Nullable
    String mqttUsername();

    /**
     * Optional password to use to authenticate to the MQTT broker.
     */
    @Nullable
    String mqttPassword();
}
