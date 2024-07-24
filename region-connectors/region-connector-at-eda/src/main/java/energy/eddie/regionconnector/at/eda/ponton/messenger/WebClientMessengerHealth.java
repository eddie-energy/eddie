package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

public class WebClientMessengerHealth implements MessengerHealth {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientMessengerHealth.class);

    private final WebClient webClient;
    private final PontonXPAdapterConfiguration config;

    public WebClientMessengerHealth(WebClient webClient, PontonXPAdapterConfiguration config) {
        this.webClient = webClient;
        this.config = config;
    }

    @Override
    public MessengerStatus messengerStatus() {
        try {
            var status = webClient.get()
                                  .uri(config.apiEndpoint() + "/health")
                                  .retrieve()
                                  .bodyToMono(MessengerStatus.class)
                                  .block();
            if (status == null) {
                return new MessengerStatus(Map.of(), false);
            }
            return status;
        } catch (WebClientResponseException e) {
            LOGGER.error("Error while checking health of Ponton XP Messenger", e);
            return new MessengerStatus(Map.of(), false);
        }
    }
}
