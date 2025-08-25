package energy.eddie.outbound.metric.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.generated.PermissionRequestMetrics;
import energy.eddie.outbound.metric.model.Metrics;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MetricsReportBuilderTest {

    @Test
    void testCreateMetricsReport_jsonInput() throws IOException {
        // Given
        MetricsReportBuilder metricsReportBuilder = new MetricsReportBuilder();
        List<PermissionRequestMetricsModel> inputData = List.of(
                new PermissionRequestMetricsModel(
                        135.0,
                        131.0,
                        3,
                        PermissionProcessStatus.CREATED,
                        "validated",
                        "Fluvius",
                        "be-fluvius"
                ),
                new PermissionRequestMetricsModel(
                        13588.0,
                        16364.0,
                        3,
                        PermissionProcessStatus.VALIDATED,
                        "validated",
                        "Fluvius",
                        "be-fluvius"
                ),
                new PermissionRequestMetricsModel(
                        2405.0,
                        349.0,
                        3,
                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                        "validated",
                        "Fluvius" ,
                        "be-fluvius"
                ),
                new PermissionRequestMetricsModel(
                        5.5,
                        5.5,
                        3,
                        PermissionProcessStatus.ACCEPTED,
                        "validated",
                        "Fluvius",
                        "be-fluvius"
                )
        );

        ObjectMapper mapper = new ObjectMapper();
        InputStream json = getClass().getClassLoader().getResourceAsStream("metrics_report_example.json");
        PermissionRequestMetrics input = mapper.readValue(json, PermissionRequestMetrics.class);

        // When
        PermissionRequestMetrics result = metricsReportBuilder.createMetricsReport("EDDIE_Test", inputData);

        // Then
        assertEquals(mapper.writeValueAsString(input), mapper.writeValueAsString(result));
    }

    @Test
    void testCreateMetricsReport_empty() {
        // Given
        MetricsReportBuilder metricsReportBuilder = new MetricsReportBuilder();

        // When
        PermissionRequestMetrics result = metricsReportBuilder.createMetricsReport("empty-instance", List.of());

        // Then
        assertEquals("empty-instance", result.getInstance());
        assertEquals(0, ((Metrics) result.getMetrics()).getCount());
        assertTrue(result.getRegionConnectorMetrics().isEmpty());
    }
}
