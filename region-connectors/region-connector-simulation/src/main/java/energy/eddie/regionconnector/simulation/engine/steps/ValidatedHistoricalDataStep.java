package energy.eddie.regionconnector.simulation.engine.steps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.ValidatedHistoricalDataEmissionStep;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

public final class ValidatedHistoricalDataStep extends Model {
    public static final String DISCRIMINATOR_VALUE = "ValidatedHistoricalDataStep";
    private final SimulatedValidatedHistoricalData meterReading;

    @JsonCreator
    public ValidatedHistoricalDataStep(SimulatedValidatedHistoricalData meterReading) {
        super(DISCRIMINATOR_VALUE);
        this.meterReading = meterReading;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        return List.of(
                new ValidatedHistoricalDataEmissionStep(meterReading)
        );
    }

    @JsonProperty
    public SimulatedValidatedHistoricalData meterReading() {return meterReading;}

    @Override
    public int hashCode() {
        return Objects.hash(meterReading);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ValidatedHistoricalDataStep) obj;
        return Objects.equals(this.meterReading, that.meterReading);
    }

    @Override
    public String toString() {
        return "ValidatedHistoricalDataStep[" +
               "meterReading=" + meterReading + ']';
    }
}
