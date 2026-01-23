// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.metric.service;

import energy.eddie.outbound.metric.config.MetricOutboundConnectorConfiguration;
import energy.eddie.outbound.metric.generated.PermissionRequestMetrics;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsReportServiceTest {

    @Mock
    private PermissionRequestMetricsRepository metricsRepository;

    @SuppressWarnings("unused")
    @Mock
    private MetricOutboundConnectorConfiguration config;

    @Mock
    private MetricsReportBuilder reportBuilder;

    private MetricsReportService service;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        service = new MetricsReportService(metricsRepository, config, reportBuilder, webClient);

        when(config.endpoint()).thenReturn(mockWebServer.url("/metadata-sharing").uri());

    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGenerateAndSendReport_Success() throws InterruptedException {
        // Given
        String eddieId = "EDDIE-Test";
        List<PermissionRequestMetricsModel> metricsList = List.of();
        PermissionRequestMetrics report = new PermissionRequestMetrics();
        report.setEddieId(eddieId);

        // When
        when(metricsRepository.findAll()).thenReturn(metricsList);
        when(config.eddieId()).thenReturn(eddieId);
        when(reportBuilder.createMetricsReport(metricsList, eddieId)).thenReturn(report);

        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        service.generateAndSendReport();

        var request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);

        ObjectMapper mapper = new ObjectMapper();
        PermissionRequestMetrics sentReport = mapper.readValue(request.getBody().readUtf8(), PermissionRequestMetrics.class);

        // Then
        assertNotNull(request);
        assertEquals("/metadata-sharing", request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals("application/json", request.getHeader("Content-Type"));

        assertNotNull(sentReport);
        assertEquals(eddieId, sentReport.getEddieId());
    }
}
