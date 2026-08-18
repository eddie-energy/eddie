// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.*;

public class DataFollowsAcceptedStepOrDataConstraint implements StructuralConstraint {
    @Override
    public ConstraintResult violatesConstraint(Model current, Model next) {
        if (next instanceof ValidatedHistoricalDataStep
            && !(isAcceptedStatusStep(current) || current instanceof ValidatedHistoricalDataStep)) {
            return new ConstraintViolation(
                    "ValidatedHistoricalDataStep must follow ACCEPTED StatusChangeStep or another ValidatedHistoricalDataStep"
            );
        }
        if (next instanceof LoadProfileCurveStep && !isAcceptedStatusStep(current)) {
            return new ConstraintViolation(
                    "LoadProfileCurveStep must follow ACCEPTED StatusChangeStep"
            );
        }
        return new ConstraintOk();
    }

    private static boolean isAcceptedStatusStep(Step current) {
        return current instanceof StatusChangeStep statusStep && statusStep.status() == PermissionProcessStatus.ACCEPTED;
    }
}
