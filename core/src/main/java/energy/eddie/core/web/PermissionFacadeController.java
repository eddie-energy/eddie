package energy.eddie.core.web;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.application.information.ApplicationInformation;
import energy.eddie.core.services.*;
import energy.eddie.dataneeds.exceptions.DataNeedDisabledException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PermissionFacadeController {
    private final ApplicationInformation applicationInformation;
    private final MetadataService metadataService;
    private final DataNeedCalculationRouter dataNeedCalculationRouter;
    private final DataNeedRuleSetRouter dataNeedRuleSetRouter;

    public PermissionFacadeController(
            ApplicationInformationService applicationInformationService,
            MetadataService metadataService,
            DataNeedCalculationRouter dataNeedCalculationRouter,
            DataNeedRuleSetRouter dataNeedRuleSetRouter
    ) {
        this.applicationInformation = applicationInformationService.applicationInformation();
        this.metadataService = metadataService;
        this.dataNeedCalculationRouter = dataNeedCalculationRouter;
        this.dataNeedRuleSetRouter = dataNeedRuleSetRouter;
    }

    @GetMapping(value = "/application-information")
    public ResponseEntity<ApplicationInformation> applicationInformation() {
        return ResponseEntity.ok(applicationInformation);
    }

    @GetMapping("/region-connectors-metadata")
    public ResponseEntity<Collection<RegionConnectorMetadata>> regionConnectorsMetadata() {
        return ResponseEntity.ok(metadataService.getRegionConnectorMetadata());
    }

    @GetMapping("/region-connectors/{region-connector}/data-needs/{data-need-id}")
    public DataNeedCalculation dataNeedCalculation(
            @PathVariable("region-connector") String regionConnector,
            @PathVariable("data-need-id") String dataNeedId
    ) throws UnknownRegionConnectorException, DataNeedNotFoundException {
        return dataNeedCalculationRouter.calculateFor(regionConnector, dataNeedId);
    }

    @GetMapping("/region-connectors/data-needs/{data-need-id}")
    public Map<String, DataNeedCalculation> dataNeedCalculations(
            @PathVariable("data-need-id") String dataNeedId
    ) throws DataNeedNotFoundException, DataNeedDisabledException {
        return dataNeedCalculationRouter.calculate(dataNeedId);
    }

    @GetMapping("/region-connectors/data-need-rule-sets")
    public Map<String, DataNeedRuleSet> supportedDataNeeds() {
        return dataNeedRuleSetRouter.dataNeedRuleSets();
    }

    @GetMapping("/region-connectors/{region-connector}/data-need-rule-set")
    public DataNeedRuleSet supportedDataNeeds(@PathVariable("region-connector") String regionConnectorId) throws UnknownRegionConnectorException {
        return dataNeedRuleSetRouter.dataNeedRuleSets(regionConnectorId);
    }
}
