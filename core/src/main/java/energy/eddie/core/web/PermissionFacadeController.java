package energy.eddie.core.web;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.HealthService;
import energy.eddie.core.services.MetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PermissionFacadeController {
    private final MetadataService metadataService;
    private final HealthService healthService;

    public PermissionFacadeController(MetadataService metadataService,
                                      HealthService healthService) {
        this.metadataService = metadataService;
        this.healthService = healthService;
    }

    @GetMapping("/region-connectors-metadata")
    public ResponseEntity<Collection<RegionConnectorMetadata>> regionConnectorsMetadata() {
        return ResponseEntity.ok(metadataService.getRegionConnectorMetadata());
    }

    @GetMapping("/region-connectors-health")
    public ResponseEntity<Map<String, Map<String, HealthState>>> regionConnectorsHealth() {
        return ResponseEntity.ok(healthService.getRegionConnectorHealth());
    }
}
