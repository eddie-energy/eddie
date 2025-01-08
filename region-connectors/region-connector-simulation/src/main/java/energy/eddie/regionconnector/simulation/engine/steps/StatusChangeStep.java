package energy.eddie.regionconnector.simulation.engine.steps;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.DelayStep;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.StatusEmissionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

public final class StatusChangeStep extends Model {
    public static final String DISCRIMINATOR_VALUE = "StatusChangeStep";
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusChangeStep.class);
    private final PermissionProcessStatus status;
    @JsonProperty("delayInSeconds")
    private final long delay;

    public StatusChangeStep(PermissionProcessStatus status, long delay) {
        super(DISCRIMINATOR_VALUE);
        this.status = status;
        this.delay = delay;
    }

    public StatusChangeStep(PermissionProcessStatus status) {
        this(status, 0);
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        LOGGER.atDebug()
              .addArgument(status)
              .addArgument(() -> Duration.of(delay, ChronoUnit.SECONDS).toString())
              .log("Executing status change to '{}' and waiting for next step for {}");
        return List.of(
                new StatusEmissionStep(status),
                new DelayStep(delay, ChronoUnit.SECONDS)
        );
    }

    @JsonProperty
    public PermissionProcessStatus status() {return status;}

    @JsonProperty("delayInSeconds")
    public long delay() {return delay;}


    @Override
    public int hashCode() {
        return Objects.hash(status, delay);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StatusChangeStep) obj;
        return Objects.equals(this.status, that.status) &&
               this.delay == that.delay;
    }

    @Override
    public String toString() {
        return "StatusChangeStep[" +
               "status=" + status + ", " +
               "delay=" + delay + ']';
    }
}
