package energy.eddie.regionconnector.nl.mijn.aansluiting.config;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(value = "region-connector.nl.mijn.aansluiting")
public class MijnAansluitingConfiguration {
    private final String keyId;
    private final String issuerUrl;
    private final ClientID clientId;
    private final Scope scope;
    private final URI redirectUrl;

    @SuppressWarnings("java:S107")
    // Config class is only instantiated by spring
    public MijnAansluitingConfiguration(
            String keyId,
            String issuerUrl,
            ClientID clientId,
            Scope scope,
            URI redirectUrl
    ) {
        this.keyId = keyId;
        this.issuerUrl = issuerUrl;
        this.clientId = clientId;
        this.scope = scope;
        this.redirectUrl = redirectUrl;
    }

    public String keyId() {return keyId;}

    public String issuerUrl() {return issuerUrl;}

    public ClientID clientId() {return clientId;}

    public Scope scope() {return scope;}

    public URI redirectUrl() {return redirectUrl;}
}
