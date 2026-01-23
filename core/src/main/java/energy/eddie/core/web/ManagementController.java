// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.web;

import energy.eddie.core.dtos.SupportedDataNeeds;
import energy.eddie.core.services.DataNeedRuleSetRouter;
import energy.eddie.core.services.SupportedFeatureService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedFeatureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
public class ManagementController {
    private final SupportedFeatureService supportedFeatureService;
    private final DataNeedRuleSetRouter dataNeedRuleSetRouter;

    public ManagementController(
            SupportedFeatureService supportedFeatureService,
            DataNeedRuleSetRouter dataNeedRuleSetRouter
    ) {
        this.supportedFeatureService = supportedFeatureService;
        this.dataNeedRuleSetRouter = dataNeedRuleSetRouter;
    }

    @GetMapping("/${eddie.management.server.urlprefix}/region-connectors/supported-features")
    public ResponseEntity<List<RegionConnectorSupportedFeatureExtension>> supportedFeatures() {
        return ResponseEntity.ok(supportedFeatureService.getSupportedFeatureExtensions());
    }

    @GetMapping("/${eddie.management.server.urlprefix}/region-connectors/supported-data-needs")
    public ResponseEntity<Set<SupportedDataNeeds>> supportedDataNeeds() {
        return ResponseEntity.ok(dataNeedRuleSetRouter.supportedDataNeeds());
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
