package energy.eddie.core.web;

import energy.eddie.core.services.SupportedDataNeedService;
import energy.eddie.core.services.SupportedFeatureService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedDataNeedExtension;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedFeatureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class ManagementController {
    private final SupportedFeatureService supportedFeatureService;
    private final SupportedDataNeedService supportedDataNeedService;

    public ManagementController(SupportedFeatureService supportedFeatureService, SupportedDataNeedService supportedDataNeedService) {
        this.supportedFeatureService = supportedFeatureService;
        this.supportedDataNeedService = supportedDataNeedService;
    }

    @GetMapping("/${eddie.management.server.urlprefix}/region-connectors/supported-features")
    public ResponseEntity<List<RegionConnectorSupportedFeatureExtension>> supportedFeatures() {
        return ResponseEntity.ok(supportedFeatureService.getSupportedFeatureExtensions());
    }

    @GetMapping("/${eddie.management.server.urlprefix}/region-connectors/supported-data-needs")
    public ResponseEntity<List<RegionConnectorSupportedDataNeedExtension>> supportedDataNeeds() {
        return ResponseEntity.ok(supportedDataNeedService.getSupportedDataNeedExtensions());
    }

    /**
     * This is required, since spring picks up the thymeleaf dependency from the admin-console module and automatically returns the index template from the admin-console when the index of the webserver is requested.
     * By overriding the index, we can force spring not to return the index template.
     * For more information, see <a href="https://github.com/eddie-energy/eddie/issues/1365">GH-1365</a>
     */
    @GetMapping
    public void index() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
