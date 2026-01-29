// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

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
import java.util.Map;

@RestController
public class ScenarioController {
    private final SimulationEngine simulationEngine;
    private final PredefinedScenarios predefinedScenarios;

    public ScenarioController(SimulationEngine simulationEngine, PredefinedScenarios predefinedScenarios) {
        this.simulationEngine = simulationEngine;
        this.predefinedScenarios = predefinedScenarios;
    }

    @GetMapping(value = "/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<String>> predefinedScenarios() {
        return ResponseEntity.ok(predefinedScenarios.scenarioNames());
    }

    @PostMapping(value = "/scenarios/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> runScenario(@RequestBody ScenarioRunConfiguration configuration) {
        return executeScenario(configuration.metadata(), configuration.scenario());
    }

    @PostMapping(value = "/scenarios/{name}/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> runScenario(
            @PathVariable String name,
            @RequestBody ScenarioMetadata metadata
    ) {
        var scenario = predefinedScenarios.getScenario(name);
        if (scenario.isPresent()) {
            return executeScenario(metadata, scenario.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Map<String, Object>> executeScenario(ScenarioMetadata metadata, Scenario scenario) {
        var res = simulationEngine.run(scenario, metadata);
        return switch (res) {
            case SimulationConstraintViolations(List<ConstraintViolation> violations) ->
                    ResponseEntity.badRequest().body(Map.of("errors", toEddieApiError(violations)));

            case SimulationStarted ignored -> ResponseEntity.ok(Map.of("permissionId", metadata.permissionId()));
        };
    }

    private static List<EddieApiError> toEddieApiError(List<ConstraintViolation> violations) {
        return violations.stream().map(v -> new EddieApiError(v.message())).toList();
    }
}
