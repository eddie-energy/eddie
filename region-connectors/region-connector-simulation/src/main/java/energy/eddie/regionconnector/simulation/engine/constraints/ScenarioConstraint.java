package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;

public class ScenarioConstraint implements ElementConstraint {

    @Override
    public ConstraintResult violatesConstraint(Model model) {
        if (!(model instanceof Scenario scenario)) {
            return new ConstraintOk();
        }
        var first = scenario.steps().getFirst();
        if (!(first instanceof StatusChangeStep statusStep) || statusStep.status() != PermissionProcessStatus.CREATED) {
            return new ConstraintViolation("First step in scenario must be a StatusStep with created status");
        }
        var last = scenario.steps().getLast();
        if (!(last instanceof StatusChangeStep lastStep)) {
            return new ConstraintViolation("Last step in scenario must be a StatusStep with a final status");
        }
        var state = PermissionProcessState.create(lastStep.status());
        if (!state.isFinalState()) {
            return new ConstraintViolation("Last step in scenario must have a final status");
        }
        return new ConstraintOk();
    }
}
