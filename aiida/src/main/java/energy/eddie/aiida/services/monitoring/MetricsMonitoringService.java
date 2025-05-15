package energy.eddie.aiida.services.monitoring;

import energy.eddie.aiida.communication.RestInterceptor;
import energy.eddie.aiida.config.MonitoringAgentConfiguration;
import energy.eddie.aiida.models.monitoring.metrics.HostMetrics;
import energy.eddie.aiida.models.monitoring.metrics.ServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MetricsMonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsMonitoringService.class);

    private static final String SERVICE_NAME_AGENT = "aiida-agent";
    private static final String HOST_METRICS_PATH = "/metrics/host";
    private static final String SERVICE_METRICS_PATH = "/metrics/services";

    private final MonitoringAgentConfiguration config;
    private final RestTemplate restTemplate;
    private final HttpHeaders headers;
    private final HttpEntity<Void> entity;

    public MetricsMonitoringService(MonitoringAgentConfiguration config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.headers = new HttpHeaders();

        headers.set(RestInterceptor.SERVICE_NAME_HEADER, SERVICE_NAME_AGENT);
        this.entity = new HttpEntity<>(headers);
    }

    public Optional<HostMetrics> getHostMetrics() {
        return fetch(config.host() + HOST_METRICS_PATH, HostMetrics.class);
    }

    public Optional<List<ServiceMetrics>> getServiceMetrics() {
        //return fetchList(config.host() + SERVICE_METRICS_PATH, new ParameterizedTypeReference<>() {});
        double randomCpu = ThreadLocalRandom.current().nextDouble(1, 20); // e.g. 0.031
        double randomMemory = ThreadLocalRandom.current().nextDouble(1, 20); // 0.35
        double randomNetIn = ThreadLocalRandom.current().nextDouble(20_000, 25000); // Bps
        double randomNetOut = ThreadLocalRandom.current().nextDouble(5_000, 7_000); // Bps

        ServiceMetrics demo = new ServiceMetrics(
                ZonedDateTime.now(),
                "aiida-core-service-7c8987676f-mrlgq",
                "Running",
                randomCpu,
                "%",
                randomMemory,
                "%",
                randomNetOut,
                "Bps",
                randomNetIn,
                "Bps",
                98.5,
                "%"
        );

        return Optional.of(List.of(demo));
    }


    private <T> Optional<T> fetch(String url, Class<T> responseType) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            LOGGER.error("Failed to fetch [{}]: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> fetchList(String url, ParameterizedTypeReference<T> typeRef) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    typeRef
            );
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            LOGGER.error("Failed to fetch [{}]: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
