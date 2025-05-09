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

import java.util.List;
import java.util.Optional;

@Service
public class MetricsMonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsMonitoringService.class);

    private static final String SERVICE_NAME_AGENT = "aiida-agent";
    private static final String HOST_METRICS_PATH = "/host-metrics";
    private static final String SERVICE_METRICS_PATH = "/service-metrics";

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
        return fetchList(config.host() + SERVICE_METRICS_PATH, new ParameterizedTypeReference<>() {});
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
