package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.xml.helper.Status;
import org.naesb.espi.ServiceStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class GreenButtonClient implements GreenButtonApi {
    private static final String SERVICE_STATUS = "ServiceStatusApi";
    private final WebClient webClient;

    public GreenButtonClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<ServiceStatus> readServiceStatus() {
        synchronized (webClient) {
            return webClient.get()
                            .uri("/ReadServiceStatus")
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
