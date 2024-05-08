package energy.eddie.regionconnector.nl.mijn.aansluiting.client;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApiClient {
    private static final String MIJN_AANSLUITING = "MIJN_AANSLUITING";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);
    private final WebClient client = WebClient.create();
    private final Map<String, HealthState> health = new HashMap<>();

    public ApiClient() {
        health.put(MIJN_AANSLUITING, HealthState.UP);
    }

    public Mono<List<MijnAansluitingResponse>> fetchConsumptionData(String singleSyncUri, String accessToken) {
        return client.get()
                     .uri(singleSyncUri)
                     .headers(h -> h.setBearerAuth(accessToken))
                     .retrieve()
                     .bodyToMono(new ParameterizedTypeReference<List<MijnAansluitingResponse>>() {})
                     .doOnError(exception -> {
                         LOGGER.warn("Data fetching failed", exception);
                         health.put(MIJN_AANSLUITING, HealthState.DOWN);
                     })
                     .doOnNext(ignored -> health.put(MIJN_AANSLUITING, HealthState.UP));
    }

    public Map<String, HealthState> health() {
        return health;
    }
}
