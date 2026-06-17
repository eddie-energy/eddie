// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.Step;

import java.time.Duration;
import java.util.List;
import java.util.SequencedCollection;

public class WaitForTerminationStep implements Step {
    private final Duration waitFor;

    public WaitForTerminationStep(Duration waitFor) {
        this.waitFor = waitFor;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) throws ExecutionException {
        try {
            var res = ctx.documentStreams()
                         .getTerminationStream()
                         .any(permissionId -> permissionId.equals(ctx.permissionId()))
                         .timeout(waitFor)
                         .block();
            if (Boolean.TRUE.equals(res)) {
                return List.of(new StatusEmissionStep(PermissionProcessStatus.TERMINATED));
            }
        } catch (RuntimeException ignored) {
            // No Op
        }
        throw new ExecutionException("Never received termination request from eligible party for permission request " + ctx.permissionId());
    }
}
