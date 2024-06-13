package energy.eddie.regionconnector.us.green.button.api;

import energy.eddie.api.v0.HealthState;
import org.naesb.espi.ServiceStatus;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface GreenButtonApi {
    Mono<ServiceStatus> readServiceStatus();

    Mono<Map<String, HealthState>> health();
}
