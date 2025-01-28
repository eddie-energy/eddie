package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.green.button.api.TokenApi;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingApiTokenException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class OAuthTokenClientFactory {
    private final GreenButtonConfiguration configuration;
    private final Map<String, TokenApi> clients = new HashMap<>();
    private final WebClient webClient;

    public OAuthTokenClientFactory(
            GreenButtonConfiguration configuration,
            WebClient webClient
    ) {
        this.configuration = configuration;
        this.webClient = webClient;
    }

    public TokenApi create(
            String companyId,
            String jumpOffUrl
    ) throws MissingClientIdException, MissingClientSecretException, MissingApiTokenException {
        if (clients.containsKey(companyId)) {
            return clients.get(companyId);
        }
        String clientId = configuration.getClientIdOrThrow(companyId);
        String clientSecret = configuration.getClientSecretOrThrow(companyId);
        configuration.throwOnMissingToken(companyId);
        var client = new OAuthTokenClient(jumpOffUrl,
                                          clientId,
                                          clientSecret,
                                          webClient);
        clients.put(companyId, client);
        return client;
    }
}
