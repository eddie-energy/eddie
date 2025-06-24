package energy.eddie.regionconnector.nl.mijn.aansluiting.config;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(value = "region-connector.nl.mijn.aansluiting")
public record MijnAansluitingConfiguration(
        String continuousKeyId,
        String singleKeyId,
        String issuerUrl,
        ClientID continuousClientId,
        ClientID singleClientId,
        Scope singleScope,
        Scope continuousScope,
        URI codeboekApi,
        String codeboekApiToken,
        URI redirectUrl
) {
}
