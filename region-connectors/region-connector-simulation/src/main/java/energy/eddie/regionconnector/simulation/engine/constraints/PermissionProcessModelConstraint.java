package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;

public class PermissionProcessModelConstraint implements StructuralConstraint {

    @Override
    public ConstraintResult violatesConstraint(Model current, Model next) {
        if (current instanceof StatusChangeStep currentStatus && next instanceof StatusChangeStep nextStatus) {
            var currentState = PermissionProcessState.create(currentStatus.status());
            if (!currentState.nextIsAllowed(nextStatus.status())) {
                return new ConstraintViolation("Status %s is not an allowed next status for %s".formatted(
                        currentState.status(),
                        nextStatus.status()));
            }
        }
        return new ConstraintOk();
    }
}
