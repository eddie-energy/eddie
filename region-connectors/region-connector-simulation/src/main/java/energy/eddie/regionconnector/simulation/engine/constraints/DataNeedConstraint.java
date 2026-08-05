// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotFoundResult;
import energy.eddie.api.agnostic.data.needs.DataNeedNotSupportedResult;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;

public class DataNeedConstraint implements ElementConstraint {
    private final DataNeedCalculationService dataNeedsService;
    private final SimulationContext ctx;

    public DataNeedConstraint(DataNeedCalculationService dataNeedsService, SimulationContext ctx) {
        this.dataNeedsService = dataNeedsService;
        this.ctx = ctx;
    }

    @Override
    public ConstraintResult violatesConstraint(Model model) {
        if (!(model instanceof ValidatedHistoricalDataStep)) {return new ConstraintOk();}
        var dataNeed = dataNeedsService.calculate(ctx.dataNeedId(), ctx.creationDateTime());
        return switch (dataNeed) {
            case DataNeedNotFoundResult ignored ->
                    new ConstraintViolation("Data need %s does not exist".formatted(ctx.dataNeedId()));
            case DataNeedNotSupportedResult ignored ->
                    new ConstraintViolation("Data need %s does not exist".formatted(ctx.dataNeedId()));
            case ValidatedHistoricalDataDataNeedResult ignored -> new ConstraintOk();
            default -> new ConstraintViolation(
                    "Data need %s is not of type ValidatedHistoricalDataDataNeed, required by %s"
                            .formatted(ctx.dataNeedId(), model.getClass().getSimpleName())
            );
        };
    }
}
