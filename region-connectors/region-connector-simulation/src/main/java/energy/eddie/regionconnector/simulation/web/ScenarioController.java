package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.regionconnector.simulation.dtos.ScenarioRunConfiguration;
import energy.eddie.regionconnector.simulation.engine.SimulationEngine;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.results.SimulationConstraintViolations;
import energy.eddie.regionconnector.simulation.engine.results.SimulationStarted;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScenarioController {
    private final SimulationEngine simulationEngine;

    public ScenarioController(SimulationEngine simulationEngine) {this.simulationEngine = simulationEngine;}

    @PostMapping(value = "/scenarios/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EddieApiError>> runScenario(@RequestBody ScenarioRunConfiguration configuration) {
        var res = simulationEngine.run(configuration.scenario(), configuration.metadata());
        return switch (res) {
            case SimulationConstraintViolations(List<ConstraintViolation> violations) ->
                    ResponseEntity.badRequest().body(toEddieApiError(violations));

            case SimulationStarted ignored -> ResponseEntity.ok().build();
        };
    }

    private static List<EddieApiError> toEddieApiError(List<ConstraintViolation> violations) {
        return violations.stream().map(v -> new EddieApiError(v.message())).toList();
    }
}
