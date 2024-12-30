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
    private final long delay;
    private final ChronoUnit delayUnit;

    public StatusChangeStep(PermissionProcessStatus status, long delay, ChronoUnit delayUnit) {
        super(DISCRIMINATOR_VALUE);
        this.status = status;
        this.delay = delay;
        this.delayUnit = delayUnit;
    }

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

    @JsonProperty
    public PermissionProcessStatus status() {return status;}

    @JsonProperty
    public long delay() {return delay;}

    @JsonProperty
    public ChronoUnit delayUnit() {return delayUnit;}

    @Override
    public int hashCode() {
        return Objects.hash(status, delay, delayUnit);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StatusChangeStep) obj;
        return Objects.equals(this.status, that.status) &&
               this.delay == that.delay &&
               Objects.equals(this.delayUnit, that.delayUnit);
    }

    @Override
    public String toString() {
        return "StatusChangeStep[" +
               "status=" + status + ", " +
               "delay=" + delay + ", " +
               "delayUnit=" + delayUnit + ']';
    }
}
