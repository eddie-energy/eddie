// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;

import java.time.ZoneOffset;

public class ValidatedHistoricalDataStepConstraint implements ElementConstraint {
    private final SimulationContext ctx;

    public ValidatedHistoricalDataStepConstraint(SimulationContext ctx) {this.ctx = ctx;}

    @Override
    public ConstraintResult violatesConstraint(Model model) {
        if (!(ctx.calculationResult() instanceof ValidatedHistoricalDataDataNeedResult res) || !(model instanceof ValidatedHistoricalDataStep step)) {
            return new ConstraintOk();
        }
        var start = res.energyTimeframe().start().atStartOfDay(ZoneOffset.UTC);
        var end = res.energyTimeframe().end().plusDays(1).atStartOfDay(ZoneOffset.UTC);
        var meterReading = step.meterReading();
        boolean invalidStart = meterReading.startDateTime()
                                           .map(x -> x.isBefore(start))
                                           .orElse(false);
        if (invalidStart) {
            return new ConstraintViolation("Start of step is not within the energy timeframe");
        }
        boolean invalidEnd = meterReading.end()
                                         .map(x -> x.isAfter(end))
                                         .orElse(false);
        if (invalidEnd) {
            return new ConstraintViolation("End of step is not within the energy timeframe");
        }
        return new ConstraintOk();
    }
}
