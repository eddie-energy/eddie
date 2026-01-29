package energy.eddie.outbound.metric.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.generated.PermissionRequestMetrics;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsReportBuilderTest {

    @Test
    void testCreateMetricsReport_jsonInput() {
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
                        "Fluvius",
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
        PermissionRequestMetrics result = metricsReportBuilder.createMetricsReport(inputData, input.getEddieId());

        // Then
        assertEquals(mapper.writeValueAsString(input), mapper.writeValueAsString(result));
    }

    @Test
    void testCreateMetricsReport_empty() {
        // Given
        MetricsReportBuilder metricsReportBuilder = new MetricsReportBuilder();

        // When
        PermissionRequestMetrics result = metricsReportBuilder.createMetricsReport(List.of(), "EDDIE-Test");

        // Then
        assertEquals(0, result.getCount());
        assertTrue(result.getRegionConnectorMetrics().isEmpty());
    }
}
