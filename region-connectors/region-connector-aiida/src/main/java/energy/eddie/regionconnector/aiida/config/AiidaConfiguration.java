package energy.eddie.regionconnector.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

/**
 * The main configuration for the AIIDA region connector.
 *
 * @param customerId     Customer Identifier
 * @param bCryptStrength Strength to be used by {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}.
 * @param mqttServerUri  URI of the MQTT broker to which termination requests should be published.
 * @param mqttUsername   Username to use to authenticate to the MQTT broker.
 * @param mqttPassword   Optional password to use to authenticate to the MQTT broker.
 * @see <a href="https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCrypt.html">Spring documentation for BCryptPasswordEncoder</a>
 */
@ConfigurationProperties("region-connector.aiida")
public record AiidaConfiguration(
        @Name("customer.id") String customerId,
        @Name("bcrypt.strength") int bCryptStrength,
        @Name("mqtt.server.uri") String mqttServerUri,
        @Name("mqtt.username") @DefaultValue("eddie") String mqttUsername,
        @Name("mqtt.password") @DefaultValue("") String mqttPassword
) {}
