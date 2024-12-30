package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;

public class ScenarioNotNestedConstraint implements ElementConstraint {
    @Override
    public ConstraintResult violatesConstraint(Model model) {
        if (!(model instanceof Scenario scenario)) {
            return new ConstraintOk();
        }
        for (var item : scenario.steps()) {
            if (item instanceof Scenario) {
                return new ConstraintViolation("Scenarios cannot be nested");
            }
        }
        return new ConstraintOk();
    }
}
