// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.LoadProfileCurveStep;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.loadcurve.StandardProfiles;

public class LoadProfileCurveStepConstraint implements ElementConstraint {

    @Override
    public ConstraintResult violatesConstraint(Model model) {
        if (!(model instanceof LoadProfileCurveStep step)) {
            return new ConstraintOk();
        }
        var stdProfiles = StandardProfiles.getInstance();
        var allProfiles = String.join(", ", stdProfiles.allProfiles());
        var unknownProfiles = step.allProfiles()
                                  .stream()
                                  .filter(profile -> stdProfiles.getProfile(profile).isEmpty())
                                  .toList();
        if (unknownProfiles.isEmpty()) {
            return new ConstraintOk();
        }
        return new ConstraintViolation("Profiles [%s] are unknown, available profiles are: [%s]"
                                               .formatted(String.join(", ", unknownProfiles), allProfiles));
    }
}
