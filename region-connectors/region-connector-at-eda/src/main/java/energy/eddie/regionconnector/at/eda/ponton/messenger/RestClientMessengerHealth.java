package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

public class RestClientMessengerHealth implements MessengerHealth {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientMessengerHealth.class);

    private final RestClient restClient;
    private final PontonXPAdapterConfiguration config;

    public RestClientMessengerHealth(RestClient restClient, PontonXPAdapterConfiguration config) {
        this.restClient = restClient;
        this.config = config;
    }

    @Override
    public MessengerStatus messengerStatus() {
        try {
            var status = restClient.get()
                                   .uri(config.apiEndpoint() + "/health")
                                   .retrieve()
                                   .body(MessengerStatus.class);
            if (status == null) {
                return new MessengerStatus(Map.of(), false);
            }
            return status;
        } catch (RestClientResponseException e) {
            LOGGER.error("Error while checking health of Ponton XP Messenger", e);
            return new MessengerStatus(Map.of(), false);
        }
    }
}
