package energy.eddie.outbound.metric.service;

import energy.eddie.outbound.metric.config.MetricOutboundConnectorConfiguration;
import energy.eddie.outbound.metric.generated.PermissionRequestMetrics;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;

@Service
public class MetricsReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReportService.class);

    private final PermissionRequestMetricsRepository metricsRepository;
    private final MetricOutboundConnectorConfiguration config;
    private final MetricsReportBuilder reportBuilder;
    private final WebClient webClient;

    public MetricsReportService(PermissionRequestMetricsRepository metricsRepository,
                                MetricOutboundConnectorConfiguration config,
                                MetricsReportBuilder reportBuilder,
                                WebClient webClient) {
        this.metricsRepository = metricsRepository;
        this.config = config;
        this.reportBuilder = reportBuilder;
        this.webClient = webClient;
    }

    @Scheduled(cron = "${outbound-connector.metric.interval:0 0 */12 * * *}")
    void generateAndSendReport() {
        List<PermissionRequestMetricsModel> rows = metricsRepository.findAll();
        PermissionRequestMetrics report = reportBuilder.createMetricsReport(rows, config.eddieId());

        URI endpoint = config.endpoint();
        webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(report)
                .retrieve()
                .toBodilessEntity()
                .block();

        LOGGER.info("Metric report sent to endpoint {}", endpoint);
    }
}
