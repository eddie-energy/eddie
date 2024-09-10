package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.green.button.api.TokenApi;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OAuthTokenClientFactory {
    private final GreenButtonConfiguration configuration;
    private final Map<String, TokenApi> clients = new HashMap<>();

    public OAuthTokenClientFactory(GreenButtonConfiguration configuration) {this.configuration = configuration;}

    public TokenApi create(
            String companyId,
            String jumpOffUrl
    ) throws MissingClientIdException, MissingClientSecretException {
        if (clients.containsKey(companyId)) {
            return clients.get(companyId);
        }
        String clientId;
        String clientSecret;
        if (configuration.clientIds().containsKey(companyId)) {
            clientId = configuration.clientIds().get(companyId);
        } else {
            throw new MissingClientIdException();
        }

        if (configuration.clientSecrets().containsKey(companyId)) {
            clientSecret = configuration.clientSecrets().get(companyId);
        } else {
            throw new MissingClientSecretException();
        }
        var client = new OAuthTokenClient(jumpOffUrl,
                                          clientId,
                                          clientSecret,
                                          configuration);
        clients.put(companyId, client);
        return client;
    }
}
