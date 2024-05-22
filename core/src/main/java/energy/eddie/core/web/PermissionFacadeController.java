package energy.eddie.core.web;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.DataNeedCalculationRouter;
import energy.eddie.core.services.HealthService;
import energy.eddie.core.services.MetadataService;
import energy.eddie.core.services.UnknownRegionConnectorException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
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
    private final MetadataService metadataService;
    private final HealthService healthService;
    private final DataNeedCalculationRouter dataNeedCalculationRouter;

    public PermissionFacadeController(
            MetadataService metadataService,
            HealthService healthService,
            DataNeedCalculationRouter dataNeedCalculationRouter
    ) {
        this.metadataService = metadataService;
        this.healthService = healthService;
        this.dataNeedCalculationRouter = dataNeedCalculationRouter;
    }

    @GetMapping("/region-connectors-metadata")
    public ResponseEntity<Collection<RegionConnectorMetadata>> regionConnectorsMetadata() {
        return ResponseEntity.ok(metadataService.getRegionConnectorMetadata());
    }

    @GetMapping("/region-connectors-health")
    public ResponseEntity<Map<String, Map<String, HealthState>>> regionConnectorsHealth() {
        return ResponseEntity.ok(healthService.getRegionConnectorHealth());
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
    ) throws DataNeedNotFoundException {
        return dataNeedCalculationRouter.calculate(dataNeedId);
    }
}
