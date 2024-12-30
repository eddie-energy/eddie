package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.regionconnector.simulation.dtos.ScenarioMetadata;
import energy.eddie.regionconnector.simulation.dtos.ScenarioRunConfiguration;
import energy.eddie.regionconnector.simulation.engine.PredefinedScenarios;
import energy.eddie.regionconnector.simulation.engine.SimulationEngine;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.results.SimulationConstraintViolations;
import energy.eddie.regionconnector.simulation.engine.results.SimulationStarted;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
public class ScenarioController {
    private final SimulationEngine simulationEngine;
    private final PredefinedScenarios predefinedScenarios;

    public ScenarioController(SimulationEngine simulationEngine, PredefinedScenarios predefinedScenarios) {
        this.simulationEngine = simulationEngine;
        this.predefinedScenarios = predefinedScenarios;
    }

    @GetMapping("/scenarios")
    public ResponseEntity<Collection<String>> predefinedScenarios() {
        return ResponseEntity.ok(predefinedScenarios.scenarioNames());
    }

    @PostMapping(value = "/scenarios/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EddieApiError>> runScenario(@RequestBody ScenarioRunConfiguration configuration) {
        return executeScenario(configuration.metadata(), configuration.scenario());
    }

    @PostMapping(value = "/scenarios/{name}/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EddieApiError>> runScenario(
            @PathVariable String name,
            @RequestBody ScenarioMetadata metadata
    ) {
        try {
            var scenario = predefinedScenarios.getScenario(name);
            return executeScenario(metadata, scenario);
        } catch (PredefinedScenarios.ScenarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<List<EddieApiError>> executeScenario(ScenarioMetadata metadata, Scenario scenario) {
        var res = simulationEngine.run(scenario, metadata);
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
