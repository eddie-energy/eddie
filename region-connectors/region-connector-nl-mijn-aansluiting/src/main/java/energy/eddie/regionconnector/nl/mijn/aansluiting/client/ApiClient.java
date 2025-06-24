package energy.eddie.regionconnector.nl.mijn.aansluiting.client;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.ConsumptionData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class ApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);
    private final WebClient client;
    private Health health = Health.unknown().build();

    public ApiClient(WebClient.Builder builder) {this.client = builder.build();}

    public Mono<List<MijnAansluitingResponse>> fetchConsumptionData(String singleSyncUri, String accessToken) {
        return fetch(singleSyncUri, accessToken)
                .bodyToMono(new ParameterizedTypeReference<List<MijnAansluitingResponse>>() {})
                .doOnError(Exception.class, exception -> {
                    LOGGER.warn("Data fetching failed", exception);
                    health = Health.down(exception).build();
                })
                .doOnNext(ignored -> health = Health.up().build());
    }

    public Mono<List<ConsumptionData>> fetchSingleReading(String singleSyncUri, String accessToken) {
        return fetch(singleSyncUri, accessToken)
                .bodyToMono(new ParameterizedTypeReference<List<ConsumptionData>>() {})
                .doOnError(Exception.class, exception -> {
                    LOGGER.warn("Data fetching failed", exception);
                    health = Health.down(exception).build();
                })
                .doOnNext(ignored -> health = Health.up().build());
    }

    public Health health() {
        return health;
    }

    private WebClient.ResponseSpec fetch(
            String singleSyncUri,
            String accessToken
    ) {
        return client.get()
                     .uri(singleSyncUri)
                     .headers(h -> h.setBearerAuth(accessToken))
                     .retrieve();
    }
}
