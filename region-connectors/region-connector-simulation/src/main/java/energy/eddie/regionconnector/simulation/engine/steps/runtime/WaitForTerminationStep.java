// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.Step;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.SequencedCollection;

public class WaitForTerminationStep implements Step {
    private final Duration waitFor;

    public WaitForTerminationStep(Duration waitFor) {
        this.waitFor = waitFor;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        try {
            return ctx.documentStreams().getTerminationStream()
                      .any(permissionId -> permissionId.equals(ctx.permissionId()))
                      .timeout(waitFor)
                      .onErrorResume(throwable -> Mono.just(false))
                      .map(WaitForTerminationStep::onTermination)
                      .blockOptional()
                      .orElseGet(List::of);
        } catch (IllegalStateException e) {
            return List.of();
        }
    }

    private static @NonNull SequencedCollection<Step> onTermination(boolean present) {
        return present
                ? List.of(new StatusEmissionStep(PermissionProcessStatus.TERMINATED))
                : List.of();
    }
}
