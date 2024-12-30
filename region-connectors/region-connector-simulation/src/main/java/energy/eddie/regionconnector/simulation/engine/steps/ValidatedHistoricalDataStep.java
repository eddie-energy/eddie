package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.ValidatedHistoricalDataEmissionStep;

import java.util.List;
import java.util.SequencedCollection;

public class ValidatedHistoricalDataStep implements Model {
    private final SimulatedValidatedHistoricalData meterReading;

    public ValidatedHistoricalDataStep(SimulatedValidatedHistoricalData meterReading) {this.meterReading = meterReading;}

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        return List.of(
                new ValidatedHistoricalDataEmissionStep(meterReading)
        );
    }
}
