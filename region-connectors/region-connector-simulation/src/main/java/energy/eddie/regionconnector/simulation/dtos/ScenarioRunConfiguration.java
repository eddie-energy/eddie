package energy.eddie.regionconnector.simulation.dtos;

import energy.eddie.regionconnector.simulation.engine.steps.Scenario;

public record ScenarioRunConfiguration(Scenario scenario, ScenarioMetadata metadata) {
}
