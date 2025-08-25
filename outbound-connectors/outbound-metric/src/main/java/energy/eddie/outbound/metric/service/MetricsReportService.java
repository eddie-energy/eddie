package energy.eddie.outbound.metric.service;

import energy.eddie.outbound.metric.config.MetricOutboundConnectorConfiguration;
import energy.eddie.outbound.metric.generated.*;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@SuppressWarnings("FutureReturnValueIgnored")
@Service
public class MetricsReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReportService.class);

    private final PermissionRequestMetricsRepository metricsRepository;
    private final MetricOutboundConnectorConfiguration config;
    private final RestTemplate restTemplate;
    private final MetricsReportBuilder reportBuilder;

    public MetricsReportService(PermissionRequestMetricsRepository metricsRepository,
                                MetricOutboundConnectorConfiguration config,
                                RestTemplate restTemplate,
                                MetricsReportBuilder reportBuilder,
                                TaskScheduler taskScheduler) {
        this.metricsRepository = metricsRepository;
        this.config = config;
        this.restTemplate = restTemplate;
        this.reportBuilder = reportBuilder;
        taskScheduler.scheduleAtFixedRate(this::fetchMetricsReport, config.interval());
    }

    void fetchMetricsReport() {
        generateAndSendReport(config.instance(), config.endpoint());
    }

    void generateAndSendReport(String instance, URI endpoint) {
        List<PermissionRequestMetricsModel> rows = metricsRepository.findAll();
        PermissionRequestMetrics report = reportBuilder.createMetricsReport(instance, rows);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.exchange(endpoint, HttpMethod.POST, new HttpEntity<>(report, headers), Void.class);
        LOGGER.info("Metric report sent to endpoint {}", endpoint);
    }
}
