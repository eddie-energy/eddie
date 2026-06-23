// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.SequencedCollection;

public class WaitForTerminationStep implements Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForTerminationStep.class);
    private final Duration waitFor;

    public WaitForTerminationStep(Duration waitFor) {
        this.waitFor = waitFor;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) throws ExecutionException {
        var permissionId = ctx.permissionId();
        try {
            LOGGER.debug("Waiting for termination request from eligible party for permission request {}",
                         permissionId);
            var res = terminationReceived(ctx);
            if (res) {
                return List.of(new StatusEmissionStep(PermissionProcessStatus.TERMINATED));
            }
            LOGGER.debug("No termination request received from eligible party for permission request {}", permissionId);
        } catch (RuntimeException e) {
            LOGGER.info(
                    "Exception occurred while waiting for termination request from eligible party for permission request {}",
                    permissionId,
                    e
            );
        }
        throw new ExecutionException("Never received termination request from eligible party for permission request " + permissionId);
    }

    private boolean terminationReceived(SimulationContext ctx) {
        var permissionId = ctx.permissionId();
        var res = ctx.documentStreams()
                     .getTerminationStream()
                     .doOnComplete(() -> LOGGER.error(
                             "Stream unexpectedly completed, while waiting for termination request {}",
                             permissionId
                     ))
                     .any(permissionId::equals)
                     .timeout(waitFor)
                     .block();
        return res != null && res;
    }
}
