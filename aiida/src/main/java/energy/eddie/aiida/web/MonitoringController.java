package energy.eddie.aiida.web;

import energy.eddie.aiida.models.monitoring.HostMetrics;
import energy.eddie.aiida.models.monitoring.ServiceMetrics;
import energy.eddie.aiida.services.monitoring.MonitoringMetricsService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitoring")
@OpenAPIDefinition(info = @Info(title = "Monitoring API", version = "1.0", description = "Monitoring of AIIDA, Host and Services"))
public class MonitoringController {
    private final MonitoringMetricsService monitoringMetricsService;

    @Autowired
    public MonitoringController(MonitoringMetricsService monitoringMetricsService) {
        this.monitoringMetricsService = monitoringMetricsService;
    }

    @Operation(summary = "Get host metrics", description = "Retrieve all host metrics (hostname, cpuUsage, memoryUsage).",
            operationId = "getHostMetrics", tags = {"host-metrics"})
    @GetMapping("/host-metrics")
    public ResponseEntity<HostMetrics> getHostMetrics() {
        return monitoringMetricsService.getHostMetrics()
                                       .map(ResponseEntity::ok)
                                       .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @Operation(summary = "Get service metrics", description = "Retrieve all service metrics (name, status, cpuUsage, memoryUsage).",
            operationId = "getServiceMetrics", tags = {"service-metrics"})
    @GetMapping("/service-metrics")
    public ResponseEntity<List<ServiceMetrics>> getServiceMetrics() {
        return monitoringMetricsService.getServiceMetrics()
                                       .map(ResponseEntity::ok)
                                       .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
