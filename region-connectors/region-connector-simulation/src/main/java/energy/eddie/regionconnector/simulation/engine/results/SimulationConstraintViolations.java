package energy.eddie.regionconnector.simulation.engine.results;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;

import java.util.List;

public record SimulationConstraintViolations(List<ConstraintViolation> violations) implements SimulationResult {
}
