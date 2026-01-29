// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.Step;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;

public class DataFollowsAcceptedStepOrDataConstraint implements StructuralConstraint {
    @Override
    public ConstraintResult violatesConstraint(Model current, Model next) {
        if (next instanceof ValidatedHistoricalDataStep
            && !(isAcceptedStatusStep(current) || current instanceof ValidatedHistoricalDataStep)) {
            return new ConstraintViolation(
                    "ValidatedHistoricalDataStep must follow ACCEPTED StatusChangeStep or another ValidatedHistoricalDataStep"
            );
        }
        return new ConstraintOk();
    }

    private static boolean isAcceptedStatusStep(Step current) {
        return current instanceof StatusChangeStep statusStep && statusStep.status() == PermissionProcessStatus.ACCEPTED;
    }
}
