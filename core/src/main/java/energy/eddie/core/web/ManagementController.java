package energy.eddie.core.web;

import energy.eddie.core.services.SupportedFeatureService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedFeatureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ManagementController {
    private final SupportedFeatureService supportedFeatureService;

    public ManagementController(SupportedFeatureService supportedFeatureService) {this.supportedFeatureService = supportedFeatureService;}

    @GetMapping("/${eddie.management.server.urlprefix}/region-connectors/supported-features")
    public ResponseEntity<List<RegionConnectorSupportedFeatureExtension>> supportedFeatures() {
        return ResponseEntity.ok(supportedFeatureService.getSupportedFeatureExtensions());
    }
}
