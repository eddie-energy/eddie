package energy.eddie.regionconnector.aiida.config;

import jakarta.annotation.Nullable;
import org.springframework.web.util.UriTemplate;

/**
 * @param customerId     Customer Identifier
 * @param bCryptStrength Strength to be used by {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}.
 * @param handshakeUrl   URL which should be used by AIIDA instances as an endpoint for the handshake.
 *                       Is a template with a placeholder {@code permissionId} that can be replaced by using {@link UriTemplate}.
 *                       A PATCH request to this URL can be used to change the state of the permission request (e.g. to ACCEPTED), whereas a GET request will return the details of the permission request.
 * @param mqttServerUri  URI of the MQTT broker to which termination requests should be published.
 * @param mqttPassword   Optional password to use to authenticate to the MQTT broker.
 * @see <a href="https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCrypt.html">Spring documentation for BCryptPasswordEncoder</a>
 */
public record AiidaConfiguration(
        String customerId,
        int bCryptStrength,
        String handshakeUrl,
        String mqttServerUri,
        @Nullable String mqttPassword
) {
    public static final String PREFIX = "region-connector.aiida.";
    public static final String CUSTOMER_ID = PREFIX + "customer.id";
    public static final String BCRYPT_STRENGTH = PREFIX + "bcrypt.strength";
    public static final String MQTT_SERVER_URI = PREFIX + "mqtt.server.uri";
    public static final String MQTT_PASSWORD = PREFIX + "mqtt.password";
    public static final String EDDIE_PUBLIC_URL = "eddie.public.url";
    private static final String USERNAME = "eddie";

    /**
     * Username to use to authenticate to the MQTT broker.
     */
    public String mqttUsername() {
        return USERNAME;
    }
}
