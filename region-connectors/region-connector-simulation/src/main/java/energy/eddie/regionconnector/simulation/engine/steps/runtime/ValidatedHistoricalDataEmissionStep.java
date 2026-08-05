// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.Step;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

public class ValidatedHistoricalDataEmissionStep implements Step {
    private final SimulatedValidatedHistoricalData meterReading;

    public ValidatedHistoricalDataEmissionStep(SimulatedValidatedHistoricalData meterReading) {
        this.meterReading = meterReading;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) throws ExecutionException {
        var start = switch (ctx.calculationResult()) {
            case ValidatedHistoricalDataDataNeedResult x ->
                    Optional.of(x.energyTimeframe().start().atStartOfDay(ZoneOffset.UTC));
            default -> meterReading.startDateTime();
        };
        var document = new SimulatedMeterReading(
                ctx.connectionId(),
                ctx.dataNeedId(),
                ctx.permissionId(),
                meterReading.meteringPoint(),
                start.orElseThrow(() -> new ExecutionException(
                        "Neither start date via data need nor via scenario provided")),
                meterReading.meteringInterval().toString(),
                meterReading.measurements()
        );
        ctx.documentStreams()
           .publish(document);
        return List.of();
    }

    @Override
    public int hashCode() {
        return meterReading.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ValidatedHistoricalDataEmissionStep that)) return false;

        return meterReading.equals(that.meterReading);
    }
}
