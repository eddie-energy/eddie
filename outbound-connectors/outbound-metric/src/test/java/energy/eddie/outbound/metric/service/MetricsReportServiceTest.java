package energy.eddie.outbound.metric.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import energy.eddie.outbound.metric.config.MetricOutboundConnectorConfiguration;
import energy.eddie.outbound.metric.generated.PermissionRequestMetrics;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsReportServiceTest {

    @Mock
    private PermissionRequestMetricsRepository metricsRepository;

    @SuppressWarnings("unused")
    @Mock
    private MetricOutboundConnectorConfiguration config;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MetricsReportBuilder reportBuilder;

    @SuppressWarnings("unused")
    @Mock
    private TaskScheduler taskScheduler;

    @Spy
    @InjectMocks
    private MetricsReportService service;

    @Test
    void testGenerateAndSendReport_Success() throws JsonProcessingException {
        // Given
        String instance = "EDDIE-test";
        URI endpoint = URI.create("https://eddie.energy/metadata-sharing");
        List<PermissionRequestMetricsModel> metricsList = List.of();
        PermissionRequestMetrics report = new PermissionRequestMetrics();

        // When
        when(metricsRepository.findAll()).thenReturn(metricsList);
        when(reportBuilder.createMetricsReport(instance, metricsList)).thenReturn(report);

        service.generateAndSendReport(instance, endpoint);

        var httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(endpoint), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(Void.class));

        // Then
        var capturedEntity = httpEntityCaptor.getValue();
        assertEquals(report, capturedEntity.getBody());
        assertEquals(MediaType.APPLICATION_JSON, capturedEntity.getHeaders().getContentType());
    }

    @Test
    void testFetchMetricsReport_HandlesJsonException() throws JsonProcessingException {
        // Given
        String instance = "EDDIE-test";
        URI endpoint = URI.create("https://eddie.energy/metadata-sharing");

        // When
        when(config.instance()).thenReturn(instance);
        when(config.endpoint()).thenReturn(endpoint);

        doThrow(new JsonProcessingException("Simulated error") {})
                .when(service).generateAndSendReport(instance, endpoint);

        // Then
        assertDoesNotThrow(() -> service.fetchMetricsReport());
        verify(service).generateAndSendReport(instance, endpoint);
    }
}
