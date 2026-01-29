// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;

public class DataNeedConstraint implements ElementConstraint {
    private final DataNeedsService dataNeedsService;
    private final SimulationContext ctx;

    public DataNeedConstraint(DataNeedsService dataNeedsService, SimulationContext ctx) {
        this.dataNeedsService = dataNeedsService;
        this.ctx = ctx;
    }

    @Override
    public ConstraintResult violatesConstraint(Model model) {
        switch (model) {
            case ValidatedHistoricalDataStep ignored -> {
                var dataNeed = dataNeedsService.findById(ctx.dataNeedId());
                if (dataNeed.isEmpty()) {
                    return new ConstraintViolation("Data need %s does not exist".formatted(ctx.dataNeedId()));
                }
                var dn = dataNeed.get();
                if (!(dn instanceof ValidatedHistoricalDataDataNeed)) {
                    return new ConstraintViolation(
                            "Data need %s is not of type ValidatedHistoricalDataDataNeed, required by %s"
                                    .formatted(ctx.dataNeedId(), model.getClass().getSimpleName())
                    );
                }
            }
            default -> {
                // No-Op
            }
        }
        return new ConstraintOk();
    }
}
