package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.DelayStep;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.StatusEmissionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.SequencedCollection;

public record StatusChangeStep(PermissionProcessStatus status, long delay, ChronoUnit delayUnit) implements Model {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusChangeStep.class);

    public StatusChangeStep(PermissionProcessStatus status) {
        this(status, 0, ChronoUnit.SECONDS);
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        LOGGER.atDebug()
              .addArgument(status)
              .addArgument(() -> Duration.of(delay, delayUnit).toString())
              .log("Executing status change to '{}' and waiting for next step for {}");
        return List.of(
                new StatusEmissionStep(status),
                new DelayStep(delay, delayUnit)
        );
    }
}
