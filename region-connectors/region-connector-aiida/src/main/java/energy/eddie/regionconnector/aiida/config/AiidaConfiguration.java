package energy.eddie.regionconnector.aiida.config;

public interface AiidaConfiguration {
    String PREFIX = "region-connector.aiida.";
    String CUSTOMER_ID = PREFIX + "customer.id";
    String BCRYPT_STRENGTH = PREFIX + "bcrypt.strength";
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
     * {@code permissionId} that can be replaced by using {@link org.springframework.web.util.UriTemplate}.
     */
    String handshakeUrl();
}
