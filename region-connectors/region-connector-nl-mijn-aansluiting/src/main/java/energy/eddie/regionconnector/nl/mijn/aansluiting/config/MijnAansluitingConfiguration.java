package energy.eddie.regionconnector.nl.mijn.aansluiting.config;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(value = "region-connector.nl.mijn.aansluiting")
public class MijnAansluitingConfiguration {
    private final String continuousKeyId;
    private final String singleKeyId;
    private final String issuerUrl;
    private final ClientID continuousClientId;
    private final ClientID singleClientId;
    private final Scope singleScope;
    private final Scope continuousScope;
    private final URI redirectUrl;

    @SuppressWarnings("java:S107")
    // Config class is only instantiated by spring
    public MijnAansluitingConfiguration(
            String continuousKeyId,
            String singleKeyId,
            String issuerUrl,
            ClientID continuousClientId,
            ClientID singleClientId,
            Scope singleScope,
            Scope continuousScope,
            URI redirectUrl
    ) {
        this.continuousKeyId = continuousKeyId;
        this.singleKeyId = singleKeyId;
        this.issuerUrl = issuerUrl;
        this.continuousClientId = continuousClientId;
        this.singleClientId = singleClientId;
        this.singleScope = singleScope;
        this.continuousScope = continuousScope;
        this.redirectUrl = redirectUrl;
    }

    public String continuousKeyId() {return continuousKeyId;}

    public String singleKeyId() {return singleKeyId;}

    public String issuerUrl() {return issuerUrl;}

    public ClientID continuousClientId() {return continuousClientId;}

    public ClientID singleClientId() {return singleClientId;}

    public Scope singleScope() {return singleScope;}

    public Scope continuousScope() {return continuousScope;}

    public URI redirectUrl() {return redirectUrl;}
}
