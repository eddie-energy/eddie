package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.xml.helper.Status;
import org.naesb.espi.ServiceStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class GreenButtonClient implements GreenButtonApi {
    private static final String SERVICE_STATUS = "ServiceStatusApi";
    private final WebClient webClient;
    private final GreenButtonConfiguration configuration;

    public GreenButtonClient(WebClient webClient, GreenButtonConfiguration configuration) {
        this.webClient = webClient;
        this.configuration = configuration;
    }

    @Override
    public Mono<ServiceStatus> readServiceStatus() {
        synchronized (webClient) {
            return webClient.get()
                            .uri("/ReadServiceStatus")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + configuration.apiToken())
                            .retrieve()
                            .bodyToMono(ServiceStatus.class);
        }
    }

    @Override
    public Mono<Boolean> isAlive() {
        return readServiceStatus().map(serviceStatus -> !Status.fromValue(serviceStatus.getCurrentStatus())
                                                               .equals(Status.UNAVAILABLE));
    }

    @Override
    public Mono<Map<String, HealthState>> health() {
        return readServiceStatus().map(serviceStatus -> {
            if (Status.fromValue(serviceStatus.getCurrentStatus()).equals(Status.UNAVAILABLE)) {
                return Map.of(SERVICE_STATUS, HealthState.DOWN);
            }
            return Map.of(SERVICE_STATUS, HealthState.UP);
        });
    }
}
